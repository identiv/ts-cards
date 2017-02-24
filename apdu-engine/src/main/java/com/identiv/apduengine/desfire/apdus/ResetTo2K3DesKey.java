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
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Reset Key #0 to default 2K3DES key. Assumes current session is AES key.
 */
public class ResetTo2K3DesKey extends DesfireApduCommand {

    Logger logger = LoggerFactory.getLogger(ResetTo2K3DesKey.class);

    static byte[] TWOK3DES_KEY_DEFAULT = new byte[16];

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        byte keyNo = (byte) 0x00;

        byte[] newKey = TWOK3DES_KEY_DEFAULT;

        // C4 keyNo newKey
        byte[] crcData = new byte[18];
        crcData[0] = (byte) 0xC4;
        crcData[1] = keyNo;
        System.arraycopy(newKey, 0, crcData, 2, 16);
        byte[] crc32 = DesfireUtils.calCrc32(crcData);
        byte[] cryptogram = new byte[32];
        System.arraycopy(newKey, 0, cryptogram, 0, newKey.length);
        System.arraycopy(crc32, 0, cryptogram, 16, crc32.length);

        byte[] sessionKey = getSession()
                .getRequiredParameter(DesfireAuth.SESSION_KEY)
                .getValueAsBytes();

        byte[] cryptoEnc = DesfireUtils.aesEncrypt(getSession(),cryptogram, sessionKey);

        byte[] apdu = new byte[33];
        apdu[0] = keyNo;
        System.arraycopy(cryptoEnc, 0, apdu, 1, cryptoEnc.length);
        logger.info("**** Session Key : " + Hex.encode(sessionKey));
        logger.info("**** Key No **** : " + Hex.encode(new byte[] { keyNo }));
//        logger.info("**** New Key *** : " + Hex.encode(newKey));
        logger.info("**** Crc32 ***** : " + Hex.encode(crc32));
//        logger.info("**** Cryptogram  : " + Hex.encode(cryptogram));
        logger.info("**** CryptoEnc * : " + Hex.encode(cryptoEnc));
        return Lists.newArrayList(
                new DesfireCommand(0xC4, apdu)
        );
    }

    @Override
    public void validateResponse(ApduResponse response) {
        DesfireAuth.resetAuthentication(getSession());
        super.validateResponse(response);
    }
}
