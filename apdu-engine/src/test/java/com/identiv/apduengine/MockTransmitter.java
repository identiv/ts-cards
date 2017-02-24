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
package com.identiv.apduengine;

import com.google.common.primitives.Bytes;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.desfire.apdus.DesfireKeyType;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.ApduTransmitter;
import com.identiv.apduengine.engine.util.Hex;

import javax.smartcardio.CommandAPDU;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.identiv.apduengine.desfire.apdus.DesfireKeyType.AES_128;
import static com.identiv.apduengine.desfire.apdus.DesfireKeyType.TWO_KEY_3DES;

/**
 * This mocks transmission to a DESFire EV1 card. It acts like a very limited
 * virtual PICC. It does not a lot but it does allow us to effectively test
 * authentication etc.
 */
public class MockTransmitter implements ApduTransmitter {

    byte[] rndB;
    private byte[] key;

    private ApduSession session = new ApduSession();
    private DesfireKeyType keyType;

    public MockTransmitter() {
        this(TWO_KEY_3DES, new byte[24]);
    }

    public MockTransmitter(DesfireKeyType keyType) {
        this(keyType, new byte[keyType.getKeySize()]);
    }

    public MockTransmitter(DesfireKeyType keyType, byte[] key) {
        this.keyType = keyType;
        this.key = key;
        rndB = new byte[keyType.getChallengeSize()];
        DesfireAuthTest.secureRandom.nextBytes(rndB);
    }

    boolean isAes = false;
    @Override
    public ListenableFuture<ApduResponse> apply(byte[] in) {
        // if this is an authentication command set the key type and crypto
        // mode for the following commands
        if (Arrays.equals(in, DesfireAuthTest.AUTH_APDU)) {
            session.setParameter(
                    DesfireAuth.KEY_TYPE_KEY, TWO_KEY_3DES.name());
        } else if (Arrays.equals(in, DesfireAuthTest.AUTH_AES_APDU)) {
            session.setParameter(
                    DesfireAuth.KEY_TYPE_KEY, AES_128.name());
            isAes = true;
        }

        if (DesfireAuth.isAuthenticated(session)) {
            // calculate CMAC on inbound data
            CommandAPDU commandAPDU = new CommandAPDU(in);
            byte[] payload = Bytes.concat(
                    new byte[] { Integer.valueOf(commandAPDU.getINS()).byteValue() },
                    commandAPDU.getData()
            );
            System.out.println("PICC Calculating CMAC from " + Hex.encode(payload));

            byte[] cmac = DesfireUtils.cmac(session, payload, true);

            System.out.println("PICC CMAC is " + Hex.encode(cmac));
        }


        if (Arrays.equals(in, DesfireAuthTest.AUTH_APDU) ||
            Arrays.equals(in, DesfireAuthTest.AUTH_AES_APDU)) {

            try {
                System.out.println("PICC RndB:     " + Hex.encode(rndB));

                byte[] rndBEncrypted = isAes ?
                        DesfireUtils.aesEncrypt(session, rndB, key) :
                        DesfireUtils.desEncrypt(session, rndB, key);
                System.out.println("PICC RndB Enc: " + Hex.encode(rndBEncrypted));
                return Futures.immediateFuture(new ApduResponse(
                        (byte) 0x91,
                        (byte) 0xAF,
                        rndBEncrypted));
            } catch (RuntimeException e) {
                System.out.println("Authentication error: " + e.getMessage());
                session.removeParameter(DesfireAuth.INIT_VEC_KEY);
                return Futures.immediateFuture(new ApduResponse(
                        (byte) 0x91,
                        (byte) 0xAE));
            }

        } else if (Bytes.indexOf(in, DesfireAuthTest.AUTH_RNDAB_APDU) == 0) {
            ByteBuffer inBB = ByteBuffer.wrap(in);
            inBB.position(DesfireAuthTest.AUTH_RNDAB_APDU.length + 1);
            byte[] rndABEnc = new byte[keyType.getChallengeSize() * 2];

            inBB.get(rndABEnc);

            byte[] rndAB = isAes ?
                    DesfireUtils.aesDecrypt(session, rndABEnc, key) :
                    DesfireUtils.desDecrypt(session, rndABEnc, key);

            System.out.println("PICC RndAB:    " + Hex.encode(rndAB));

            byte[] rndA = new byte[keyType.getChallengeSize()];
            System.arraycopy(rndAB, 0, rndA, 0, keyType.getChallengeSize());
            byte[] rndBRotated = new byte[keyType.getChallengeSize()];
            System.arraycopy(rndAB, keyType.getChallengeSize(),
                    rndBRotated, 0, keyType.getChallengeSize());

            System.out.println("PICC RndB':    " + Hex.encode(rndBRotated));

            byte[] rndBFromTerminal = DesfireUtils.unrotate(rndBRotated);

            System.out.println("PICC RndB:     " + Hex.encode(rndBFromTerminal));

            if (!Arrays.equals(rndB, rndBFromTerminal)) {

                System.out.println("Rejecting terminal!");
                System.out.println();

                return Futures.immediateFuture(new ApduResponse((byte) 0x91, (byte) 0xAE));
            } else {

                System.out.println("OK we have authenticated terminal! Now to send back RndA...");

                // so now we have to send back RndA
                System.out.println("PICC RndA:     " + Hex.encode(rndA));
                byte[] rndARotated = DesfireUtils.rotate(rndA);
                System.out.println("PICC RndA':    " + Hex.encode(rndARotated));
                byte[] rndAEnc = isAes ?
                        DesfireUtils.aesEncrypt(session, rndARotated, key):
                        DesfireUtils.desEncrypt(session, rndARotated, key);
                System.out.println("PICC RndA Enc: " + Hex.encode(rndAEnc));

                byte[] sessionKey = DesfireUtils.makeSessionKey(keyType,
                        rndA, rndB);

                System.out.println("PICC SessionKey : " + Hex.encode(sessionKey) +
                        " (" + keyType + ")");

                // also need to tell the session that we are now authenticated
                session.setParameter(DesfireAuth.STAGE_KEY,
                        DesfireAuth.Stage.AUTHENTICATED.name());
                session.setParameter(DesfireAuth.SESSION_KEY,
                        sessionKey);
                session.removeParameter(DesfireAuth.INIT_VEC_KEY);

                return Futures.immediateFuture(new ApduResponse((byte) 0x91, (byte) 0x00, rndAEnc));
            }

        } else if (Arrays.equals(in, DesfireAuthTest.FORMAT_CARD) ||
                   Arrays.equals(in, DesfireAuthTest.SELECT_PICC) ||
                   Arrays.equals(in, DesfireAuthTest.GET_APPLICATION_IDS) ||
                   Arrays.equals(in, DesfireAuthTest.GET_KEY_VERSION)) {
            if (!DesfireAuth.isAuthenticated(session)) {
                return Futures.immediateFuture(
                        new ApduResponse((byte) 0x91, (byte) 0x00));
            } else {
                byte[] payload = new byte[1];

                System.out.println("PICC Calculating CMAC from response of " + Hex.encode(payload));

                byte[] cmac = DesfireUtils.cmac(session, new byte[1], true);
                System.out.println("PICC Response CMAC is " + Hex.encode(cmac));

                return Futures.immediateFuture(new ApduResponse(
                        (byte) 0x91, (byte) 0x00,
                        Arrays.copyOfRange(cmac, 0, 8)));
            }
        } else {
            throw new RuntimeException("ApduCommand not supported yet [" +
                    Hex.encode(in) + "]");
        }
    }
}
