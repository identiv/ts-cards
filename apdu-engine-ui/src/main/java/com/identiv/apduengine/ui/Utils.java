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
package com.identiv.apduengine.ui;

import com.google.common.primitives.Bytes;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.desfire.apdus.DesfireKeyType;
import com.identiv.apduengine.desfire.crypto.JceAesCryptor;
import com.identiv.apduengine.engine.util.Hex;

import java.util.Collections;
import java.util.List;

public class Utils {

    public static byte[] reverseArray(byte[] array) {
        List<Byte> list = Bytes.asList(array);
        Collections.reverse(list);
        return Bytes.toArray(list);
    }

    public static DesfireAuth buildAuthAppCmd(boolean useKeyDiv, String uid,
            String aid, String key) {
        DesfireAuth cmdDesfireAuth;
        if (useKeyDiv) {
            cmdDesfireAuth = new DesfireAuth(DesfireUtils.cmacKeyDivAes128(
                    Bytes.concat(Hex.decode(uid), reverseArray(Hex.decode(aid))),
                    new JceAesCryptor(Hex.decode(key))),
                    DesfireKeyType.AES_128);
        } else {
            //TODO:
            cmdDesfireAuth = new DesfireAuth(new byte[16], DesfireKeyType.AES_128);
        }
        return cmdDesfireAuth;
    }

    public static DesfireAuth buildAuthPiccCmd(boolean useKeyDiv,
            boolean isAES, String uid, String key) {
        DesfireAuth cmdDesfireAuth;
        if (useKeyDiv) {
            cmdDesfireAuth = new DesfireAuth(DesfireUtils.cmacKeyDivAes128(
                    Hex.decode(uid),
                    new JceAesCryptor(Hex.decode(key))),
                    DesfireKeyType.AES_128);
        } else {
            if ((key == null || key.isEmpty()) && !isAES) {
                cmdDesfireAuth = new DesfireAuth(
                        new byte[24], DesfireKeyType.TWO_KEY_3DES);
            } else {
                cmdDesfireAuth = new DesfireAuth(buildKeyAesNoDiv(key),
                        DesfireKeyType.AES_128);
            }
        }
        return cmdDesfireAuth;
    }

    public static byte[] buildKeyAesNoDiv(String key) {
        if (key == null || key.isEmpty()) {
            return new byte[16];
        }
        byte[] keyArr = Hex.decode(key);
        if (keyArr.length < 16) {
            byte[] tmp = new byte[16];
            for (int i=0; i<keyArr.length; i++) {
                tmp[i] = keyArr[i];
            }
            return tmp;
        }
        return keyArr;
    }

}
