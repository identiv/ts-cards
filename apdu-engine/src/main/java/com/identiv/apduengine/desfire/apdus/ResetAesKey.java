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
 * Change Key #0 to default AES key. Assumes current session is AES key.
 */
public class ResetAesKey extends DesfireApduCommand {


    Logger logger = LoggerFactory.getLogger(ResetAesKey.class);

    static final byte[] AES_KEY_DEFAULT = new byte[16];

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        byte keyNo = (byte) 0x80;
        byte keyVersion = 0x00;

        byte[] newKey = AES_KEY_DEFAULT;

        DesfireKeyType sessionType = DesfireKeyType.valueOf(getSession()
                .getKeyTypeParameter(true, getIndex())
                .getValue());

        // C4 keyNo newKey keyVersion
        byte[] crcData = new byte[19];
        crcData[0] = (byte) 0xC4;
        crcData[1] = keyNo;
        System.arraycopy(newKey, 0, crcData, 2, 16);
        crcData[18] = keyVersion;
        byte[] crc32 = DesfireUtils.calCrc32(crcData);

        int cryptogramLength = sessionType == DesfireKeyType.AES_128
                ? 32 : 24;
        byte[] cryptogram = new byte[cryptogramLength];
        System.arraycopy(newKey, 0, cryptogram, 0, newKey.length);
        cryptogram[16] = keyVersion;
        System.arraycopy(crc32, 0, cryptogram, 17, crc32.length);

        byte[] sessionKey = getSession()
                .getRequiredParameter(DesfireAuth.SESSION_KEY)
                .getValueAsBytes();

        byte[] cryptoEnc = sessionType == DesfireKeyType.AES_128
                ? DesfireUtils.aesEncrypt(cryptogram, sessionKey, new byte[16])
                : DesfireUtils.desEncrypt(cryptogram, sessionKey, new byte[8]);

        byte[] apdu = new byte[1 + cryptogramLength];
        apdu[0] = keyNo;
        System.arraycopy(cryptoEnc, 0, apdu, 1, cryptoEnc.length);
        logger.info("**** Session Key : " + Hex.encode(sessionKey));
        logger.info("**** Key No **** : " + Hex.encode(new byte[] { keyNo }));
        logger.info("**** Key Version : " + Hex.encode(new byte[] { keyVersion }));
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
