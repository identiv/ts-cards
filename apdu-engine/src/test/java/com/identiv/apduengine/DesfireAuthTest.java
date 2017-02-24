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

import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.desfire.apdus.DesfireKeyType;
import com.identiv.apduengine.desfire.apdus.SelectPicc;
import com.identiv.apduengine.engine.ApduEngine;
import com.identiv.apduengine.engine.ApduStatus;
import com.identiv.apduengine.engine.StatusResponse;
import com.identiv.apduengine.engine.util.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.SecureRandom;
import java.util.Optional;

/**
 */
public class DesfireAuthTest {

    final static byte[] AUTH_APDU = new byte[] { (byte) 0x90, 0x1A, 0x00, 0x00, 0x01, 0x00, 0x00 };
    final static byte[] AUTH_AES_APDU = new byte[] { (byte) 0x90, (byte) 0xAA, 0x00, 0x00, 0x01, 0x00, 0x00 };
    final static byte[] AUTH_RNDAB_APDU = new byte[] { (byte) 0x90, (byte) 0xAF, 0x00, 0x00 }; //, 0x10 };

    final static byte[] SELECT_PICC = Hex.decode("905A00000300000000");
    final static byte[] FORMAT_CARD = Hex.decode("90FC000000");
    final static byte[] GET_KEY_VERSION = Hex.decode("90640000010000");

    final static byte[] GET_APPLICATION_IDS = Hex.decode("906A000000");

    final static byte[] DEFAULT_KEY = new byte[24];

    static SecureRandom secureRandom = new SecureRandom();

    MockTransmitter mockTransmitter;

    @Before
    public void createTransmitter() {
        this.mockTransmitter = new MockTransmitter();
    }

    @Test
    public void simulateCardAuthentication() {

        DesfireAuth auth = new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES);
        ApduSession controller = new ApduSession(auth);
        Optional<ApduEngine> apduEngine = controller.nextApduEngine();

        while (apduEngine.isPresent()) {
            StatusResponse status = apduEngine.get().sendSequence(mockTransmitter);
            apduEngine = controller.nextApduEngine(status.getResponse().orNull());

            Assert.assertEquals(ApduStatus.SUCCESS, status.getStatus());
        }

    }


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void simulateCardAuthenticationWithWrongKeySize() {
//        thrown.expect(new ExceptionCauseMatcher(IllegalArgumentException.class));
        thrown.expect(IllegalArgumentException.class);
        DesfireAuth auth = new DesfireAuth(new byte[2], DesfireKeyType.TWO_KEY_3DES);

        ApduSession controller = new ApduSession(auth);
        Optional<ApduEngine> apduEngine = controller.nextApduEngine();
        Assert.assertTrue(apduEngine.isPresent());

        StatusResponse status;
        try {
            status = apduEngine.get().sendSequence(mockTransmitter);
        } catch (Throwable t) {
            Assert.fail("This part should succeed!");
            throw new RuntimeException();
        }

        // fails preparing response to cards's challenge
        apduEngine = controller.nextApduEngine(status.getResponse().orNull());


    }

    @Test
    public void controllerDoesItAll() {
        DesfireAuth auth = new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES);
        ApduSession controller = new ApduSession(auth);

        StatusResponse status = controller.transmit(new MockTransmitter());

        Assert.assertEquals(ApduStatus.SUCCESS, status.getStatus());
        Assert.assertTrue(auth.isAuthenticated(controller));

    }

    @Test
    public void simulateCardAuthenticationSpecificRndB() {

        // use known/specific RndB for testing
        mockTransmitter.rndB = new byte[] {
                0x4F, (byte) 0xD1, (byte) 0xB7, 0x59,
                0x42, (byte) 0xA8, (byte) 0xB8, (byte) 0xE1
        };

        DesfireAuth auth = new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES);
        ApduSession controller = new ApduSession(auth);
        Optional<ApduEngine> apduEngine = controller.nextApduEngine();

        while (apduEngine.isPresent()) {
            StatusResponse status =
                    apduEngine.get().sendSequence(mockTransmitter);
            apduEngine = controller.nextApduEngine(status.getResponse().orNull());

            Assert.assertEquals(ApduStatus.SUCCESS, status.getStatus());

        }

        Assert.assertTrue(auth.isAuthenticated(controller));

    }

    @Test
    public void encryptAndDecryptAreSymmetric() {
        byte[] in = new byte[8];
        secureRandom.nextBytes(in);

        byte[] encrypted = DesfireUtils.desEncrypt(
                new ApduSession(), in, DEFAULT_KEY);
        byte[] decrypted = DesfireUtils.desDecrypt(
                new ApduSession(), encrypted, DEFAULT_KEY);

        Assert.assertThat(in, org.hamcrest.CoreMatchers.is(decrypted));

    }

    @Test
    public void selectPiccThenAuth() {
        DesfireAuth auth = new DesfireAuth(new byte[24], DesfireKeyType.TWO_KEY_3DES);
        ApduSession session = new ApduSession(
                new SelectPicc(),
                auth);
        StatusResponse status = session.transmit(mockTransmitter);

        Assert.assertEquals(ApduStatus.SUCCESS, status.getStatus());
        Assert.assertTrue(auth.isAuthenticated(session));
    }

    @Test
    public void letsDoAesAuth() {

        DesfireAuth auth = new DesfireAuth(
                new byte[16],
                DesfireKeyType.AES_128);
        ApduSession session = new ApduSession(
                new SelectPicc(),
                auth);
        StatusResponse status = session.transmit(
                new MockTransmitter(DesfireKeyType.AES_128)
        );

        Assert.assertEquals(ApduStatus.SUCCESS, status.getStatus());
        Assert.assertTrue(auth.isAuthenticated(session));


    }

}
