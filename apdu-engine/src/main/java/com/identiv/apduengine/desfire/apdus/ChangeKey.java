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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.util.Hex;

import java.util.List;


/**
 * Change Key #0 to 128-bit AES key. Assumes current session is 2k3des.
 * <p>
 * This will be generalized in the future so that it can be used to change
 * other keys and to different types.
 */
public class ChangeKey extends DesfireApduCommand {

    static final String NEW_KEY_KEY =
            ChangeKey.class.getSimpleName() + "::NewKey";

    public ChangeKey() {
    }

    public ChangeKey(byte[] newKey) {
        Preconditions.checkArgument(newKey.length == 16,
                "Only a 128-bit AES key is supported");

        registerParameter(() -> new ApduSessionParameter(
                NEW_KEY_KEY, newKey
        ));
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        byte keyNo = (byte) 0x80;
        byte keyVersion = 0x00;

        byte[] newKey = getSession().getRequiredParameter(NEW_KEY_KEY, getIndex())
                .getValueAsBytes();

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
        logger.debug("**** Session Key : " + Hex.encode(sessionKey));
        logger.debug("**** Key No **** : " + Hex.encode(new byte[] { keyNo }));
        logger.debug("**** Key Version : " + Hex.encode(new byte[] { keyVersion }));
//        logger.debug("**** New Key *** : " + Hex.encode(newKey));
        logger.debug("**** Crc32 ***** : " + Hex.encode(crc32));
//        logger.debug("**** Cryptogram  : " + Hex.encode(cryptogram));
        logger.debug("**** CryptoEnc * : " + Hex.encode(cryptoEnc));
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
