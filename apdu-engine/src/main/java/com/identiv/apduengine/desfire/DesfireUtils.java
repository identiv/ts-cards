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
package com.identiv.apduengine.desfire;

import com.google.common.base.Throwables;
import com.google.common.primitives.Bytes;
import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.desfire.apdus.DesfireKeyType;
import com.identiv.apduengine.desfire.crypto.AesCryptor;
import com.identiv.apduengine.desfire.crypto.JceAesCryptor;
import com.identiv.apduengine.engine.util.Hex;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 */
public class DesfireUtils {

    /**
     * This might be helpful later, probably doesn't work "as is"
     */
    public static byte[] calculateCrc32(byte[]... byteArrays) {

        // precalculate size
        int size = 0;
        for (byte[] data : byteArrays) {
            size += data.length;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        for (byte[] data : byteArrays) {
            try {
                baos.write(data);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        int padding = Math.max(16, (size % 16 == 0) ? 0 : 16 - (size % 16));

        byte[] withPadding = new byte[size + padding];
        System.arraycopy(baos.toByteArray(), 0,
                withPadding, 0, baos.size());

        Checksum checksum = new CRC32();
        // update the current checksum with the specified array of bytes
        checksum.update(withPadding, 0, withPadding.length);

        // get the current checksum value
        long checksumValue = checksum.getValue();

        byte[] crc32 = Hex.decode(Long.toHexString(checksumValue).toUpperCase());
        inverseBytes(crc32);
        return crc32;

    }

    public static byte[] calculateCrc16(byte[]... byteArrays) {

        // precalculate size
        int size = 0;
        for (byte[] data : byteArrays) {
            size += data.length;
        }


        ByteArrayOutputStream baos = new ByteArrayOutputStream(size);
        for (byte[] data : byteArrays) {
            try {
                baos.write(data);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        byte[] withPadding = new byte[(8 - (size % 8)) + size];
        System.arraycopy(baos.toByteArray(), 0,
                withPadding, 0, baos.size());



        Checksum checksum = new CRC32();
        // update the current checksum with the specified array of bytes
        checksum.update(withPadding, 0, withPadding.length);

        // get the current checksum value
        long checksumValue = checksum.getValue();

        byte[] crc32 = Hex.decode(Long.toHexString(checksumValue).toUpperCase());
        inverseBytes(crc32);
        return crc32;

    }



    public static byte[] calCrc32(byte[] data) {
        Checksum checksum = new CRC32();
        // update the current checksum with the specified array of bytes
        checksum.update(data, 0, data.length);

        // get the current checksum value
        long checksumValue = checksum.getValue();

        String hex = Long.toHexString(checksumValue).toUpperCase();
        while (hex.length() < 8) {
            hex = "0" + hex;
        }
        byte[] crc32 = Hex.decode(hex);

        inverseBytes(crc32);
        reverseArray(crc32);
        return crc32;
    }

    static void inverseBytes(byte[] data) {
        for (int i=0; i<data.length; i++) {
            data[i] = (byte) (~data[i] & 0xFF);
        }
    }

    static void reverseArray(byte[] data) {
        for (int i=0; i<data.length/2; i++) {
            byte tmp = data[i];
            data[i] = data[data.length - 1 - i];
            data[data.length - 1 - i] = tmp;
        }
    }

    public static byte[] encrypt(ApduSession session, int cmdIdx, byte[] data, byte[] key) {
        DesfireKeyType type = DesfireKeyType.valueOf(
                session.getKeyTypeParameter(true, cmdIdx).getValue());
        if (type == DesfireKeyType.TWO_KEY_3DES) {
            return desEncrypt(session, data, key);
        } else if (type == DesfireKeyType.AES_128) {
            return aesEncrypt(session, data, key);
        } else {
            throw new RuntimeException("Unsupported encryption mode");
        }
    }

    public static byte[] decrypt(ApduSession session, int cmdIdx, byte[] data, byte[] key) {
        DesfireKeyType type = DesfireKeyType.valueOf(
                session.getKeyTypeParameter(true, cmdIdx).getValue());
        if (type == DesfireKeyType.TWO_KEY_3DES) {
            return desDecrypt(session, data, key);
        } else if (type == DesfireKeyType.AES_128) {
            return aesDecrypt(session, data, key);
        } else {
            throw new RuntimeException("Unsupported encryption mode");
        }
    }

    static class SubKeys {
        byte[] k0, k1, k2;

        public byte[] getK0() {
            return k0;
        }

        public void setK0(byte[] k0) {
            this.k0 = k0;
        }

        public byte[] getK1() {
            return k1;
        }

        public void setK1(byte[] k1) {
            this.k1 = k1;
        }

        public byte[] getK2() {
            return k2;
        }

        public void setK2(byte[] k2) {
            this.k2 = k2;
        }
    }

    /**
     * This method will return a diversified AES key based on the
     * diversification data provided.
     */
    public static byte[] cmacKeyDivAes128(byte[] divData,
                                          AesCryptor cryptor) {

        // Refer to NXP application note AN10922
        // http://www.nxp.com/documents/application_note/AN10922.pdf

        // steps 1-4 - generate diversification keys
        SubKeys ds = getKeys(cryptor);

        // steps 8-12 - put together m and then d
        byte[] d = new byte[32];

        // step 8 - assemble m starting from d[1]
        int divSize = Math.min(31, divData.length);
        System.arraycopy(divData, 0, d, 1, divSize);

        d[0] = 0x01; // step 9 - aes 128 starts with 0x01

        // step 10 - do we need padding?
        boolean needPadding = divSize < 31;
        // systemIdUsableLength < maxSystemIdSpace;
        if (needPadding) {
            // step 10 - yes we need padding
            // step 11 - padding is 0x80 with trailing zeros
            d[1 + divData.length] = (byte) 0x80;
        }

        // step 12 - d is "CMAC input D" (done)
        // step 13 xor last 16 bytes
        byte[] cmac = cmac(d, ds, needPadding);

        // step 14 encrypt using master key (initvec all zeros)
        byte[] encryptedCmac = cryptor.encrypt(cmac, new byte[16]);

        // step 15 take last 16 bytes
        byte[] divKey = Arrays.copyOfRange(encryptedCmac, 16, 32);

        return divKey;

    }

    private static SubKeys getKeys(AesCryptor cryptor) {

        SubKeys ds  = new SubKeys();
        byte[] key0 = cryptor.encrypt(new byte[16], new byte[16]);
        byte[] subKey1 = generateSubKey(key0);
        byte[] subKey2 = generateSubKey(subKey1);
        ds.setK0(key0);
        ds.setK1(subKey1);
        ds.setK2(subKey2);
        return ds;
    }


    public static byte[] desDecrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
            SecretKey skey = new SecretKeySpec(key, "DESede");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
            return cipher.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static byte[] desEncrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
            SecretKey skey = new SecretKeySpec(key, "DESede");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);

            return cipher.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static byte[] rotate(byte[] data) {
        byte[] out = new byte[data.length];
        System.arraycopy(data, 1,
                out, 0, data.length - 1);
        out[out.length - 1] = data[0];
        return out;
    }

    public static byte[] unrotate(byte[] data) {
        byte[] out = new byte[data.length];
        System.arraycopy(data, 0,
                out, 1, data.length - 1);
        out[0] = data[out.length - 1];
        return out;
    }

    public static byte[] aesDecrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKey skey = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
            return cipher.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static byte[] aesEncrypt(byte[] data, byte[] key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKey skey = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);

            return cipher.doFinal(data);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static byte[] desDecrypt(ApduSession session, byte[] data, byte[] key) {

        ApduSessionParameter initVec = session.getParameterOrNew(
                DesfireAuth.INIT_VEC_KEY, new byte[8]);

        byte[] out = desDecrypt(data, key,
                initVec.getValueAsBytes());

        initVec.setValue(data, data.length - 8, 8);

        return out;

    }

    public static byte[] desEncrypt(ApduSession session, byte[] data, byte[] key) {

        ApduSessionParameter initVec = session.getParameterOrNew(
                DesfireAuth.INIT_VEC_KEY, new byte[8]);

        byte[] out = desEncrypt(data, key,
                initVec.getValueAsBytes());

        initVec.setValue(out, out.length - 8, 8);

        return out;

    }


    public static byte[] aesDecrypt(ApduSession session, byte[] data, byte[] key) {

        ApduSessionParameter initVec = session.getParameterOrNew(
                DesfireAuth.INIT_VEC_KEY, new byte[16]);

        byte[] out = DesfireUtils.aesDecrypt(data, key, initVec.getValueAsBytes());

        initVec.setValue(data, data.length - 16, 16);

        return out;

    }

    public static byte[] aesEncrypt(ApduSession session, byte[] data, byte[] key) {

        ApduSessionParameter initVec = session.getParameterOrNew(
                DesfireAuth.INIT_VEC_KEY, new byte[16]);

        byte[] out = DesfireUtils.aesEncrypt(data, key, initVec.getValueAsBytes());

        initVec.setValue(out, out.length - 16, 16);

        return out;

    }

    protected static byte[] generateSubKey(byte[] key) {

        byte[] subKey = shiftLeft(key);
        int msbL = (key[0] & 0xff) >> 7;
        if (msbL == 1) {
            subKey[15] = (byte) (subKey[15] ^ (byte) 0x87);
        }

        return subKey;
    }

    public static byte[] cmac(ApduSession session, byte[] data,
                              boolean cmacPadding) {
        ApduSessionParameter initVec = session.getParameterOrNew(
                DesfireAuth.INIT_VEC_KEY, new byte[16]);

        ApduSessionParameter sessionKey = session.getRequiredParameter(
                DesfireAuth.SESSION_KEY);

        byte[] cmac = cmac(
                data,
                new JceAesCryptor(sessionKey.getValueAsBytes()),
                initVec.getValueAsBytes(),
                cmacPadding);

        initVec.setValue(cmac);

        return cmac;

    }



    public static byte[] cmac(byte[] data,
                              AesCryptor cryptor,
                              byte[] initVec,
                              boolean cmacPadding) {

        // this is a little different to the key derivation CMAC and is taken
        // directly from NIST SP 800-38b. The concept is exactly the same but
        // here it is not applied to key derivation.
        SubKeys ds = getKeys(cryptor);

        // calculate padding required, sorry for the double ternary, goes
        // like this:
        // if length == 0 -> pad to 16 bytes
        // if length % 16 == 0 -> no padding
        // else -> pad to length + (16 - (length % 16))
        int padding = data.length == 0 ? 16 : data.length % 16 == 0 ? 0 :
                16 - (data.length % 16);
        int size = data.length + padding;

        byte[] paddedData = new byte[size];
        System.arraycopy(data, 0,
                paddedData, 0, data.length);

        // only add cmac padding byte if cmacPadding flag is true and we did
        // actually add padding
        if (padding != 0 && cmacPadding) {
            // mark start of padding with 0x80 (1000 0000) then zeros until
            // end of block is reached
            paddedData[data.length] = (byte) 0x80;
        }

        // note this only works for blocks up to 32 bytes long!
        byte[] ready = cmac(paddedData, ds, padding != 0);


        byte[] encrypted = cryptor.encrypt(ready, initVec);

        // return last block
        byte[] cmac = Arrays.copyOfRange(encrypted,
                encrypted.length - 16, encrypted.length);

        return cmac;


    }

    protected static byte[] cmac(byte[] d, SubKeys ds, boolean padded) {

        /**
         * Last 16-byte is XORed with K2 if padding is added, otherwise XORed with K1
         */
        byte[] head = d.length == 16 | d.length == 0 ? new byte[0] :
                Arrays.copyOfRange(d, 0, d.length - 16);
        byte[] finalBlock = d.length == 16 | d.length == 0 ? d :
                Arrays.copyOfRange(d, d.length - 16, d.length);

        byte[] xordFinalBlock = xor(finalBlock,
                padded ? ds.getK2() : ds.getK1());

        return Bytes.concat(head, xordFinalBlock);

    }

    public static byte[] xor(byte[] data1, byte[] data2) {
        byte[] result = new byte[data1.length]; // < data2.length ? data2.length : data1.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((byte) (data1[i] ^ data2[i]));
        }

        return result;
    }

    public static byte[] shiftLeft(byte[] data) {

        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            String s = Integer.toBinaryString(0x100 + b);
            sb.append(s.subSequence(s.length() - 8, s.length()));
        }

        String s = sb.toString().substring(1) + "0";

        byte[] a = new byte[s.length() / 8];

        for (int index = 0, i = 0; i < s.length(); index++, i += 8) {
            a[index] = (byte) Integer.parseInt(s.substring(i, i + 8), 2);
        }

        return a;
    }

    public static byte[] fileLenToBytes(int fileLength) {
        ByteBuffer buffer = ByteBuffer
                .allocate(Long.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(fileLength);

        byte[] bytes = new byte[3];
        System.arraycopy(buffer.array(), 0, bytes, 0, 3);

        return bytes;
    }

    public static byte[] makeSessionKey(DesfireKeyType keyType,
                                        byte[] rndA, byte[] rndB) {
        byte[] sessionKey = new byte[keyType.getKeySize()];

        System.arraycopy(rndA, 0, sessionKey, 0, 4);
        System.arraycopy(rndB, 0, sessionKey, 4, 4);
        if (keyType == DesfireKeyType.TWO_KEY_3DES) {
            // To do 2K3DES with DESede the third key is a duplicate of the
            // first key.
            System.arraycopy(sessionKey, 0, sessionKey, 8, 8);
            System.arraycopy(sessionKey, 0, sessionKey, 16, 8);
        } else {
            System.arraycopy(rndA, 12, sessionKey, 8, 4);
            System.arraycopy(rndB, 12, sessionKey, 12, 4);
        }

        return sessionKey;

    }
}
