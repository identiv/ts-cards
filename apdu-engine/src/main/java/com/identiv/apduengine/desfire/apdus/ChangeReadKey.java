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
package com.identiv.apduengine.desfire.apdus;

import com.google.common.collect.Lists;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Change Key #0 to default AES key. Assumes current session is AES key.
 */
public class ChangeReadKey extends DesfireApduCommand {
    static final String NEW_KEY_KEY =
            ChangeReadKey.class.getSimpleName() + "::NewKey";

    Logger logger = LoggerFactory.getLogger(ChangeReadKey.class);

    public ChangeReadKey() {
    }

    public ChangeReadKey(byte[] newKey) {
        registerParameter(() -> new ApduSessionParameter(
                NEW_KEY_KEY, newKey
        ));
    }

    @Override
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return cmacOptions == CmacOptions.RECEIVING;
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        byte keyNo = (byte) 0x01;
        byte keyVersion = 0x00;

        byte[] newKey = getSession()
                .getRequiredParameter(NEW_KEY_KEY, getIndex())
                .getValueAsBytes();

        // C4 keyNo xorKey keyVersion
        byte[] crcData = new byte[19];
        crcData[0] = (byte) 0xC4;
        crcData[1] = keyNo;
        System.arraycopy(newKey, 0, crcData, 2, 16);
        crcData[18] = keyVersion;
        byte[] crc32 = DesfireUtils.calCrc32(crcData);

        byte[] crc32Key = DesfireUtils.calCrc32(newKey);

        byte[] cryptogram = new byte[32];
        System.arraycopy(newKey, 0, cryptogram, 0, newKey.length);
        cryptogram[16] = keyVersion;
        System.arraycopy(crc32, 0, cryptogram, 17, crc32.length);
        System.arraycopy(crc32Key, 0, cryptogram, 17 + 4, crc32Key.length);

        byte[] sessionKey = getSession()
                .getRequiredParameter(DesfireAuth.SESSION_KEY)
                .getValueAsBytes();

        byte[] cryptoEnc = DesfireUtils.aesEncrypt(getSession(), cryptogram, sessionKey);

        byte[] apdu = new byte[33];
        apdu[0] = keyNo;
        System.arraycopy(cryptoEnc, 0, apdu, 1, cryptoEnc.length);
        logger.debug("**** Session Key : " + Hex.encode(sessionKey));
        logger.debug("**** Key No **** : " + Hex.encode(new byte[] { keyNo }));
        logger.debug("**** Key Version : " + Hex.encode(new byte[] { keyVersion }));
//        logger.debug("**** New Key *** : " + Hex.encode(newKey));
        logger.debug("**** Crc32 ***** : " + Hex.encode(crc32));
//        logger.debug("**** Crc32 Key * : " + Hex.encode(crc32Key));
//        logger.debug("**** Cryptogram  : " + Hex.encode(cryptogram));
        logger.debug("**** CryptoEnc * : " + Hex.encode(cryptoEnc));
        return Lists.newArrayList(
                new DesfireCommand(0xC4, apdu)
        );
    }

    @Override
    public void validateResponse(ApduResponse response) {
        super.validateResponse(response);
    }
}
