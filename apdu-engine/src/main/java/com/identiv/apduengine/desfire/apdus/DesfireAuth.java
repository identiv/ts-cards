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
import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.apdus.AbstractApduCommand;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.ApduEnginePack;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.ApduStatus;
import com.identiv.apduengine.engine.StatusWord;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CommandAPDU;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Desfire authentication commands. This currently supports 2K3DES and AES
 * authentication
 */
public class DesfireAuth extends AbstractApduCommand implements Serializable {

    Logger logger = LoggerFactory.getLogger(DesfireAuth.class);

    public enum Stage {
        NONE,
        GOT_CHALLENGE,
        RECEIVED_RESPONSE,
        AUTHENTICATED,
        AUTH_FAILURE
    }

    static final String KEY_KEY =
            DesfireAuth.class.getSimpleName() + "::AuthKey";
    static final String RNDA_KEY =
            DesfireAuth.class.getSimpleName() + "::RndA";
    static final String RNDB_KEY =
            DesfireAuth.class.getSimpleName() + "::RndB";
    public static final String INIT_VEC_KEY =
            DesfireAuth.class.getSimpleName() + "::InitVec";
    static final String RESPONSE_TO_CHALLENGE_KEY =
            DesfireAuth.class.getSimpleName() + "::ResponseToChallenge";
    public static final String STAGE_KEY =
            DesfireAuth.class.getSimpleName() + "::Stage";
    public static final String SESSION_KEY =
            DesfireAuth.class.getSimpleName() + "::SessionKey";
    public static final String KEY_TYPE_KEY =
            DesfireAuth.class.getSimpleName() + "::KeyType";

    static SecureRandom secureRandom = new SecureRandom();

    public DesfireAuth(byte[] key, DesfireKeyType keyType) {
        Preconditions.checkArgument(
                (keyType == DesfireKeyType.TWO_KEY_3DES && key.length == 24) ||
                (keyType == DesfireKeyType.AES_128      && key.length == 16),
                "Key size doesn't match size of key provided");
        registerParameter(() -> new ApduSessionParameter(
            KEY_KEY, key
        ));
        registerParameter(() -> new ApduSessionParameter(
            KEY_TYPE_KEY, keyType.name()
        ));
    }

    public DesfireAuth() {
    }

    @Override
    public void reset(ApduSession session) {
        resetAuthentication(session);
    }

    public static void resetAuthentication(ApduSession session) {
        session.removeParameter(STAGE_KEY);
        session.removeParameter(SESSION_KEY);
        session.removeParameter(INIT_VEC_KEY);
    }

    @Override
    public List<ApduEnginePack> getNextApduPacks() {

        ApduSessionParameter stage = getSession().getParameterOrNew(
                STAGE_KEY, Stage.NONE.name());

        switch (Stage.valueOf(stage.getValue())) {
        case NONE:
            DesfireKeyType type = DesfireKeyType.valueOf(
                    getSession().getKeyTypeParameter(true, getIndex()).getValue());

            // request challenge
            CommandAPDU command = new CommandAPDU(
                0x90,                   // CLA
                type == DesfireKeyType.TWO_KEY_3DES ? 0x1a : 0xaa,  // INS
                0x00,                   // P1
                0x00,                   // P2
                new byte[] { 0x00 });

            return Lists.newArrayList(new ApduEnginePack(
                    getName(), withZeroLe(command),
                    new StatusWord(0xFFFF, 0x91AF)));

        case GOT_CHALLENGE:

            // respond to challenge
            ApduSessionParameter parameter =
                    getSession().getRequiredParameter(RESPONSE_TO_CHALLENGE_KEY);
            byte[] responseToChallenge = parameter.getValueAsBytes();
            command = new CommandAPDU(
                    0x90,                   // CLA
                    0xaf,                   // INS
                    0x00,                   // P1
                    0x00,                   // P2
                    responseToChallenge);

            return Lists.newArrayList(new ApduEnginePack(
                    getName(), withZeroLe(command),
                    new StatusWord(0xFFFF, 0x9100)));


        default:
        case RECEIVED_RESPONSE:
            // nothing more to do
            return new ArrayList<>();
        }

    }

    @Override
    public boolean mustValidate() {
        Optional<ApduSessionParameter> stage = getSession().getParameter(STAGE_KEY);
        boolean mustValidate = stage.isPresent() &&
                !Stage.RECEIVED_RESPONSE.name().equals(stage.get().getValue());
        return mustValidate;
    }

    @Override
    public void validateResponse(ApduResponse response) {
        super.validateResponse(response);

        ApduSessionParameter stage = getSession().getParameterOrNew(
                STAGE_KEY, Stage.NONE.name());


        switch (Stage.valueOf(stage.getValue())) {
            case NONE:
                stage.setValue(Stage.GOT_CHALLENGE.name());
                byte[] responseToChallenge = challengeResponse(
                        getSession(), response.getData());
                getSession().setParameter(RESPONSE_TO_CHALLENGE_KEY,
                                responseToChallenge);
                return; // Status.INCOMPLETE;

            case GOT_CHALLENGE:
                stage.setValue(Stage.RECEIVED_RESPONSE.name());

                // so validate it!
                ApduStatus status = validateCardResponse(getSession(), response.getData());

                if (status == ApduStatus.SUCCESS) {
                    stage.setValue(Stage.AUTHENTICATED.name());
                } else {
                    // at this point need to restart authentication completely
                    stage.setValue(Stage.AUTH_FAILURE.name());
                }

                return;

            default:
                throw new RuntimeException("Invalid command status");
        }
    }

    @Override
    public String getName() {
        return "Authenticate";
    }

    ApduStatus validateCardResponse(ApduSession session, byte[] response) {

        DesfireKeyType type = DesfireKeyType.valueOf(
                getSession().getKeyTypeParameter(true, getIndex()).getValue());

        Preconditions.checkArgument(
                (type == DesfireKeyType.TWO_KEY_3DES && response.length == 8) ||
                (type == DesfireKeyType.AES_128      && response.length == 16),
                "Invalid card response, should contain 8/16 byte payload");

        ApduSessionParameter key = session.getRequiredParameter(KEY_KEY, getIndex());
        byte[] rndAEncrypted = response;

        logger.info("RndA enc: {}", Hex.encode(rndAEncrypted));

        byte[] rndARotated = DesfireUtils.decrypt(
                session, getIndex(), rndAEncrypted, key.getValueAsBytes());
        logger.info("RndA':    {}", Hex.encode(rndARotated));

        byte[] rndAFromCard = DesfireUtils.unrotate(rndARotated);
        logger.info("RndA?:    {}", Hex.encode(rndAFromCard));

        ApduSessionParameter rndA = session.getRequiredParameter(RNDA_KEY);
        logger.info("RndA:     {}", Hex.encode(rndA.getValueAsBytes()));

        if (Arrays.equals(rndA.getValueAsBytes(), rndAFromCard)) {
            logger.info("DESFire authentication successful!");

            ApduSessionParameter rndB = session.getRequiredParameter(RNDB_KEY);

            byte[] sessionKey = DesfireUtils.makeSessionKey(
                    type, rndA.getValueAsBytes(), rndB.getValueAsBytes());


            logger.info("SessionKey : " + Hex.encode(sessionKey) +
                    " (" + type + ")");
//            System.out.println("**** InitVec : " + getSession().getRequiredParameter(INIT_VEC_KEY));

            getSession().setParameter(SESSION_KEY, sessionKey);
            getSession().removeParameter(INIT_VEC_KEY);
            getSession().removeParameter(KEY_KEY, getIndex());
            getSession().removeParameter(RNDA_KEY);
            getSession().removeParameter(RNDB_KEY);
            getSession().removeParameter(RESPONSE_TO_CHALLENGE_KEY);

            return ApduStatus.SUCCESS;
        } else {
            logger.error("~~\\* Failure! (rndA is not a match) */~~");
            return ApduStatus.FAILURE;
        }
    }

    byte[] challengeResponse(ApduSession session, byte[] challenge) {


        ApduSessionParameter key = session.getRequiredParameter(KEY_KEY, getIndex());

        byte[] rndBEncrypted = challenge;
        logger.info("RndB Enc: {}", Hex.encode(rndBEncrypted));

        byte[] rndB = DesfireUtils.decrypt(session, getIndex(), rndBEncrypted, key.getValueAsBytes());
        logger.info("RndB:     {}", Hex.encode(rndB));

        getSession().setParameter(RNDB_KEY, rndB);

        byte[] rndBRotated = DesfireUtils.rotate(rndB);
        logger.info("RndB':    {}", Hex.encode(rndBRotated));

        // these values taken from samples can be useful for testing
        /* rndA = new byte[] {
            (byte) 0x84, (byte) 0x9B, 0x36, (byte) 0xC5,
            (byte) 0xF8, (byte) 0xBF, 0x4A, 0x09
        }; */

        DesfireKeyType keyType = DesfireKeyType.valueOf(
                getSession().getKeyTypeParameter(true, getIndex()).getValue());

        byte[] rndA = new byte[keyType.challengeSize];
        secureRandom.nextBytes(rndA);

        session.setParameter(RNDA_KEY, rndA);

        logger.info("RndA:     {}", Hex.encode(rndA));

        byte[] rndAB = new byte[keyType.challengeSize * 2];
        System.arraycopy(rndA, 0, rndAB, 0, rndA.length);
        System.arraycopy(rndBRotated, 0, rndAB, rndA.length, rndBRotated.length);
        logger.info("RndAB:    {}", Hex.encode(rndAB));

        byte[] rndABEncrypted = DesfireUtils.encrypt(
                session, getIndex(), rndAB, key.getValueAsBytes());
        logger.info("RndAB En: {}", Hex.encode(rndABEncrypted));

        return rndABEncrypted;
    }

    public static boolean isAuthenticated(ApduSession session) {
        ApduSessionParameter stage = session.getParameterOrNew(
                STAGE_KEY, Stage.NONE.name());

        return Stage.AUTHENTICATED.equals(
                Stage.valueOf(stage.getValue()));
    }

}
