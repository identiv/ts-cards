/*
 * Copyright 2017 Identiv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.identiv.apduengine.cli

import com.google.common.primitives.Bytes
import com.google.protobuf.ByteString
import com.identiv.apduengine.ApduSessionParameter
import com.identiv.apduengine.SmartcardIoTransmitter
import com.identiv.apduengine.apdus.ApduCommand
import com.identiv.apduengine.desfire.DesfireUtils
import com.identiv.apduengine.desfire.apdus.*
import com.identiv.apduengine.desfire.crypto.JceAesCryptor
import com.identiv.apduengine.engine.ApduStatus
import com.identiv.apduengine.engine.ApduTransmitter
import com.identiv.apduengine.engine.util.Hex
import static com.identiv.shared.formats.OpenAccessCredentialFormat.*

import java.util.logging.LogManager

def cli = new CliBuilder(usage: 'java -jar apdu-engine-cli.jar [options]');
cli.with {
    h longOpt: 'help', 'Show usage information'
    a longOpt: 'reset-aes',    'Reset PICC to default AES key'
    c longOpt: 'create-app', args: 1, 'Create TS app with diversified keys + ' +
            'provided payload (does not change PICC master key)'
    d longOpt: 'reset-2k3des', 'Reset PICC to default 2K3DES key (takes ' +
            'precedence over --reset-aes: if both are specified then ' +
            'only --reset-2k3des takes effect)'
    k longOpt: 'get-key-settings', 'Get PICC key settings'
    l longOpt: 'change-key-settings', 'Require auth to create application'
    p longOpt: 'create-pacs-app', args: 1, 'Create TS app with diversified ' +
            'keys and wrap payload in TS credential format'
    f longOpt: 'format-card',  'Format card'
    n longOpt: 'dry-run',      'Make no destructive changes, just log'
    r longOpt: 'read-app',     'Read TS app file 0'
    v longOpt: 'get-version',  'Get Version info from card (includes UID)'
    x longOpt: 'key', args: 1, 'Specify key for PICC authentication'
    z longOpt: 'diversify-key','Diversify PICC key on UID'
}

def options = cli.parse(args)
if (args.length == 0 || !options || options.h) {
    cli.usage()
    return
}

String logConfig = """
handlers = java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level = INFO
java.util.logging.SimpleFormatter.format=[%3\$s] %5\$s%6\$s%n
"""

LogManager.getLogManager().readConfiguration(
        new ByteArrayInputStream(logConfig.getBytes()));


ApduTransmitter transmitter = SmartcardIoTransmitter.create();
ApduEngineCli aecli = new ApduEngineCli(transmitter);

def commands = []

if (options.v || options.z || options.c || options.r || options.p) {
    aecli.andThen(new GetVersion())
    println "Card UID --> " + aecli.getSession()
            .getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY).value;
}

if (options.x) {
    aecli.authenticate(Hex.decode(options.x))
} else {
    aecli.authenticate();
}

if (options.f) {
    commands << new FormatCard();
}



Optional<ApduSessionParameter> divDataParameter = aecli.getSession()
        .getParameter(DesfireGetCardUID.CARD_UID_KEY);

byte[] newReadKey, newWriteKey;

if (divDataParameter.isPresent()) {
    byte[] divData = divDataParameter.get().getValueAsBytes();

    byte[] aid = [(byte) 0xFF, 0x54, 0x53] as byte[];
    divData = Bytes.concat(divData, aid);

    String READ_ONLY_KEY = "00112233445566778899AABBCCDDEEFF";
    String READ_WRITE_KEY = "FFEEDDCCBBAA99887766554433221100";

    newReadKey = //Hex.decode(READ_ONLY_KEY);
            DesfireUtils.cmacKeyDivAes128(
            divData,
            new JceAesCryptor(Hex.decode(READ_ONLY_KEY)));
    newWriteKey = DesfireUtils.cmacKeyDivAes128(
            divData,
            new JceAesCryptor(Hex.decode(READ_WRITE_KEY)));
}

byte[] getIdentivEnvelope(byte length, byte[] wiegand) {
    CredentialEnvelope envelope = CredentialEnvelope.newBuilder()
            .setDefaultPacsRecord(
                PacsRecord.newBuilder()
                    .setWiegandData(ByteString.copyFrom(
                        Bytes.concat(
                            [ length ] as byte[],
                            wiegand)))
                    .build())
            .build();

    return envelope.toByteArray();
}


if (options.l) {
    commands << new ChangeKeySettings((byte) 0x0B);
}

if (options.k) {
    commands << new GetKeySettings();
}

if (options.c || options.p) {
    if (options.n) {
        println "DRY-RUN: would create application"
    } else {
        commands << new CreateTsApplication()
        commands << new SelectTsApplication()

        def response = aecli.andThen(commands as ApduCommand[]);
        if (response.getStatus() != ApduStatus.SUCCESS &&
                response.getResponse().get().status == 0x91DE) {
            println "\uD83D\uDD25  Application already exists 🔥"
        }

        commands.clear()

        if (response.getStatus() == ApduStatus.SUCCESS) {


            int payloadLength = 0;
            WriteData writeData;
            if (options.p) {
                byte[] raw = Hex.decode(options.p)
                byte[] payload = getIdentivEnvelope(
                        raw[0], raw[1..(raw.length - 1)] as byte[])
                println Hex.encode(payload)
                payloadLength = payload.length;
                writeData = new WriteData(payload)
            } else {
                byte[] payload = Hex.decode(options.c);
                payloadLength = payload.length;
                writeData = new WriteData(payload)
            }

            commands = [
                    new DesfireAuth(new byte[16], DesfireKeyType.AES_128),
                    new ChangeReadKey(newReadKey),
                    new SelectTsApplication(),
                    new DesfireAuth(new byte[16], DesfireKeyType.AES_128),
                    new ChangeWriteKey(newWriteKey),
                    new DesfireAuth(newWriteKey, DesfireKeyType.AES_128),
                    new CreateStdFile(payloadLength),
                    writeData ]
        }
    }
} else if (options.r) {
    commands << new SelectTsApplication()
    commands << new DesfireAuth(newWriteKey, DesfireKeyType.AES_128)
    commands << new ReadData()
    commands << new DesfireGetCardUID()
    aecli.andThen(commands as ApduCommand[]);

    commands.clear()
    commands << new DesfireGetCardUID()


} else if (options.z) {
    byte[] uid = aecli.getSession()
            .getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY)
            .getValueAsBytes();
    byte[] newKey = DesfireUtils.cmacKeyDivAes128(
            uid, new JceAesCryptor(Hex.decode("0f1e2d3c4b5a69788796a5b4c3d2e1f0")));

    if (options.n) {
        println "DRY-RUN: would change key to " + Hex.encode(newKey)
    } else {
        println "Attempting to change key to " + Hex.encode(newKey)
        aecli.andThen([new ChangeKey(newKey)] as ApduCommand[]);
    }
} else if (options.d) {
    if (options.n) {
        println "DRY-RUN: would reset to default 2K3DES key"
    } else {
        println "Resetting to default 2K3DES key"
        commands << new ResetTo2K3DesKey();
    }
} else if (options.a) {
    if (options.n) {
        println "DRY-RUN: would reset to default AES key"
    } else {
        println "Resetting to default AES key"
        commands << new ResetAesKey();
    }
}

if (!commands.empty) {
    def response = aecli.andThen(commands as ApduCommand[]);
    println response
}
