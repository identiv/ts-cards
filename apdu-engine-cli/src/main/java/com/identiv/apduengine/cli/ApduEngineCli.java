/**
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
package com.identiv.apduengine.cli;


import com.google.common.primitives.Bytes;
import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.SmartcardIoTransmitter;
import com.identiv.apduengine.apdus.ApduCommand;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.desfire.apdus.*;
import com.identiv.apduengine.desfire.crypto.JceAesCryptor;
import com.identiv.apduengine.engine.ApduStatus;
import com.identiv.apduengine.engine.ApduTransmitter;
import com.identiv.apduengine.engine.StatusResponse;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CardException;
import java.util.List;

public class ApduEngineCli {

    Logger logger = LoggerFactory.getLogger(ApduEngineCli.class);
    ApduTransmitter transmitter;
    ApduSession session = new ApduSession();

    public static void main(String[] ignored) throws CardException {
        SmartcardIoTransmitter transmitter = SmartcardIoTransmitter.create();
        new ApduEngineCli(transmitter).go();
    }

    public ApduEngineCli(ApduTransmitter transmitter) {
        this.transmitter = transmitter;
    }

    public void go() {
//        try {
//            doStuff();
//        } catch (CardException e) {
//            e.printStackTrace();
//        }

//        }
//
//    }
//
//    public ApduStatus doStuff() {

        StatusResponse status = null;
        ApduSession session = new ApduSession(new GetVersion());
        status = session.transmit(transmitter);

        byte[] piccDivData = session.getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY).getValueAsBytes();
        byte[] appDivData = Bytes.concat(piccDivData, new byte[] {(byte)0xFF, 0x54, 0x53});

        session.nextCommands(
              new SelectPicc(),
              new GetKeySettings(),
              new DesfireAuth(DesfireUtils.cmacKeyDivAes128(
                      piccDivData,
                      new JceAesCryptor(Hex.decode("0F1E2D3C4B5A69788796A5B4C3D2E1F0"))),
                      DesfireKeyType.AES_128),
              new ResetTo2K3DesKey()
              );
        status = session.transmit(transmitter);

        session.nextCommands(
                new GetVersion(),
                new SelectPicc(),
                new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES),
                new ChangeKey(DesfireUtils.cmacKeyDivAes128(
                        session.getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY).getValueAsBytes(),
                        new JceAesCryptor(Hex.decode("0F1E2D3C4B5A69788796A5B4C3D2E1F0")))),
                new DesfireAuth(
                        DesfireUtils.cmacKeyDivAes128(
                                piccDivData,
                                new JceAesCryptor(Hex.decode("0F1E2D3C4B5A69788796A5B4C3D2E1F0"))),
                        DesfireKeyType.AES_128),
                new ChangeKeySettings((byte) 0x0B),
                new FormatCard(),
                new CreateTsApplication(),
                new SelectTsApplication(),
                new DesfireAuth(new byte[16], DesfireKeyType.AES_128),
                new ChangeReadKey(DesfireUtils.cmacKeyDivAes128(
                        appDivData,
                        new JceAesCryptor(Hex.decode("00112233445566778899AABBCCDDEEFF")))),
                new ChangeWriteKey(DesfireUtils.cmacKeyDivAes128(
                        appDivData,
                        new JceAesCryptor(Hex.decode("FFEEDDCCBBAA99887766554433221100")))),
                new DesfireAuth(
                        DesfireUtils.cmacKeyDivAes128(
                                appDivData,
                                new JceAesCryptor(Hex.decode("FFEEDDCCBBAA99887766554433221100"))),
                        DesfireKeyType.AES_128),
                new CreateStdFile("0A08220625C0513DDF98".length()/2),
                new WriteData(Hex.decode("0A08220625C0513DDF98")),
                new ReadData()
            );
            session.transmit(transmitter);
//        }

//        return ApduStatus.SUCCESS;

    }




//    public StatusResponse authenticate(//ApduTransmitter transmitter,
//                                       ApduCommand... commands) {
//        ApduSession session = new ApduSession();
//        return authenticate(session, transmitter);
//    }

    public StatusResponse andThen(ApduCommand... commands) {
//        ApduSession session = new ApduSession();
//        authenticate();
        session.nextCommands(commands);
        return session.transmit(transmitter);
    }

    public void listApplications(ApduSession session, ApduTransmitter transmitter) {
        GetApplicationIds gai = new GetApplicationIds();
        session.nextCommands(gai);

        StatusResponse sr = session.transmit(transmitter);

        List<String> aids = gai.getApplicationIds();
        if (aids.isEmpty()) {
            System.out.println(">> No applications on card");
        } else {
            System.out.println(">> Applications found on card:");
            aids.forEach(aid ->
                System.out.println(">> " + aid)
            );
        }
    }

    public StatusResponse authenticate(byte[] key) {

        DesfireAuth auth = new DesfireAuth(
                key,
                key.length == 16 ? DesfireKeyType.AES_128 : DesfireKeyType.TWO_KEY_3DES);
        auth.reset(session);
        session.reset(new SelectPicc(), auth);
        return session.transmit(transmitter);
    }


    public StatusResponse authenticate() {
        new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES).reset(session);
        session.reset(new SelectPicc(), new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES));
        StatusResponse status = session.transmit(transmitter);

        if (status.getStatus() == ApduStatus.FAILURE &&
                !DesfireAuth.isAuthenticated(session)) {

            // would be nice to know exactly how/why it failed
            // assume it failed because the card is not (so try AES)
            logger.warn("\u26A0\uFE0F  Failed, possibly due to authentication, trying to " +
                    "authenticate with default AES key.");

            session.reset(new DesfireAuth(new byte[16], DesfireKeyType.AES_128));
            status = session.transmit(transmitter);
        }

        if (status.getStatus() == ApduStatus.FAILURE &&
                !DesfireAuth.isAuthenticated(session)) {

            logger.warn("\u26A0\uFE0F  Failed, possibly due to authentication," +
                    " moving to try diversified keys");

            session.reset(new GetVersion());
            status = session.transmit(transmitter);

            if (status.getStatus() == ApduStatus.SUCCESS) {
                byte[] uid = getSession()
                        .getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY)
                        .getValueAsBytes();
                byte[] newKey = DesfireUtils.cmacKeyDivAes128(
                        uid, new JceAesCryptor(Hex.decode("0f1e2d3c4b5a69788796a5b4c3d2e1f0")));

                session.reset(new DesfireAuth(newKey, DesfireKeyType.AES_128));
                status = session.transmit(transmitter);

            }

            if (status.getStatus() == ApduStatus.FAILURE &&
                    !DesfireAuth.isAuthenticated(session)) {

                logger.warn("\u26A0\uFE0F  Still failed - " +
                        " moving to try invalidly diversified test keys");

                byte[] uid = getSession()
                        .getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY)
                        .getValueAsBytes();
                byte[] newKey = DesfireUtils.cmac(uid,
                        new JceAesCryptor(Hex.decode(
                                "0f1e2d3c4b5a69788796a5b4c3d2e1f0")),
                        new byte[16],
                        true);

                session.reset(new DesfireAuth(newKey, DesfireKeyType.AES_128));
                status = session.transmit(transmitter);

            }

            if (status.getStatus() == ApduStatus.FAILURE &&
                    !DesfireAuth.isAuthenticated(session)) {

                logger.warn("\u26A0\uFE0F  Still failed - " +
                        " final invalidly diversified test keys");

                byte[] uid = getSession()
                        .getRequiredParameter(DesfireGetCardUID.CARD_UID_KEY)
                        .getValueAsBytes();
                byte[] newKey = DesfireUtils.cmac(uid,
                        new JceAesCryptor(
                            Bytes.concat(
                                    new byte[] { 0x01 },
                            Hex.decode("0f1e2d3c4b5a69788796a5b4c3d2e1f0"))),
                        new byte[16],
                        true);

                session.reset(new DesfireAuth(newKey, DesfireKeyType.AES_128));
                status = session.transmit(transmitter);

            }
        }


        if (status.getStatus() != ApduStatus.SUCCESS) {
            throw new RuntimeException("Failed to authenticate to card");
        }

        return status;

    }

    public ApduSession getSession() {
        return session;
    }
}
