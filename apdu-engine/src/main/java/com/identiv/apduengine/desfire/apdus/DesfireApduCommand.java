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

import com.google.common.primitives.Bytes;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.apdus.AbstractApduCommand;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.ApduEnginePack;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.CommandAPDU;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 */
public abstract class DesfireApduCommand extends AbstractApduCommand
        implements Serializable {

    static protected final byte[] EMPTY_DATA = new byte[0];

    Logger logger = LoggerFactory.getLogger(DesfireApduCommand.class);

    @Override
    public List<ApduEnginePack> getNextApduPacks() {

        if (isDone()) {
            return Collections.EMPTY_LIST;
        } else {

            List<DesfireCommand> commands = getDesfireCommands();
            List<ApduEnginePack> packs = commands.stream().map(command -> {
                CommandAPDU commandApdu = new CommandAPDU(
                        0x90,                      // CLA
                        command.getCommandIns(),   // INS
                        0x00,                      // P1
                        0x00,                      // P2
                        command.getData());

                // NOTE: this is incomplete and doesn't work - expect that
                // something like this will be used in the future

                if (DesfireAuth.isAuthenticated(getSession())
                        && calculateCMAC(CmacOptions.SENDING)) {

                    ApduSessionParameter keyTypeParameter = getSession()
                            .getKeyTypeParameter(true, getIndex());
                    DesfireKeyType keyType = DesfireKeyType.valueOf(
                            keyTypeParameter.getValue());
                    switch (keyType) {

                        case AES_128: {
                            // not necessarily correct).
                            byte[] payload = Bytes.concat(
                                    new byte[]{command.getCommandIns().byteValue()},
                                    command.getData());

                            if (logger.isDebugEnabled()) {
                                logger.debug("IV is {}",
                                        getSession().getParameter(DesfireAuth.INIT_VEC_KEY).orElse(new ApduSessionParameter()));
                                logger.debug("Calculating CMAC from {}", Hex.encode(payload));
                            }


                            // we don't really need the CMAC but here it is!
                            byte[] cmac = DesfireUtils.cmac(getSession(), payload, useCmacPadding());
                            logger.debug("Calculated new CMAC and updating IV: {}",
                                    Hex.encode(cmac));

                            break;

                        }

                        default:
                            // NOTE - do not (yet) understand how this case works
                            logger.warn("Note MAC calculation is not implemented for {}  ¯\\_(ツ)_/¯", keyType);
                            break;
                    }

                }

                // not authenticated, so just give plain data
                byte[] payload = withZeroLe(commandApdu);
                return new ApduEnginePack(
                        getName(), payload,
                        command.getStatusWord());

            }).collect(Collectors.toList());

            return packs;
        }
    }

    protected abstract List<DesfireCommand> getDesfireCommands();

    /**
     * Whether or not to use CMAC style padding; if this is set to false buffers
     * will be padded with zeros (rather than CMAC-style padding)
     */
    protected boolean useCmacPadding() {
        return true;
    }

    @Override
    public void validateResponse(ApduResponse response) {

        if (DesfireAuth.isAuthenticated(getSession())
                && calculateCMAC(CmacOptions.RECEIVING)) {

            ApduSessionParameter keyTypeParameter = getSession()
                    .getKeyTypeParameter(true, getIndex());
            DesfireKeyType keyType = DesfireKeyType.valueOf(
                    keyTypeParameter.getValue());
            switch (keyType) {
            case AES_128: {
                byte[] cmacSource = Bytes.concat(
                            Arrays.copyOfRange(response.getData(), 0, response.getData().length - 8),
                            new byte[1]);

                if (logger.isDebugEnabled()) {
                    logger.debug("IV is {}",
                            getSession().getRequiredParameter(DesfireAuth.INIT_VEC_KEY));
                    logger.debug("Calculating CMAC from {}", Hex.encode(cmacSource));
                }


                byte[] cmac = DesfireUtils.cmac(
                        getSession(), cmacSource, true);
                logger.warn("Calculated CMAC and updated IV: {}",
                        Hex.encode(cmac));

                // TODO there must be a more efficient way to compare ranges of
                // bytes rather than copying them to new arrays!! Can't think of
                // it right now...
                int macSize = keyType.blockSize / 2;
                byte[] received = Arrays.copyOfRange(response.getData(),
                                response.getData().length - macSize, response.getData().length);
                if (Arrays.equals(Arrays.copyOfRange(cmac, 0, macSize), received)) {
                    logger.info("CMAC calculation is a match!!!!!",
                            Hex.encode(cmac), Hex.encode(received));
                } else {
                    logger.error("CMAC calculation is not a match! Calculated {}, received {}",
                            Hex.encode(cmac), Hex.encode(received));
                    throw new RuntimeException();
                }
                break;
            }
            default:
                logger.warn("Note MAC calculation is not implemented for {}  ¯\\_(ツ)_/¯", keyType);
                break;
            }
        }

        done();
    }

    @Override
    public boolean mustValidate() {
        // must validate "on server" if we are authenticated (in order to
        // update init vecs etc)
        return DesfireAuth.isAuthenticated(getSession());
    }

    protected void done() {
        getSession().setParameter(this.getClass().getSimpleName() +
                "::Done" + "_" + getIndex(),
                Boolean.TRUE.toString());
    }

    void undone() {
        getSession().removeParameter(this.getClass().getSimpleName() +
                "::Done" + "_" + getIndex());

    }

    public enum CmacOptions {
        SENDING,
        RECEIVING
    }
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return true;
    }

    protected boolean isDone() {
        Optional<ApduSessionParameter> done =
                getSession().getParameter(this.getClass().getSimpleName() +
                        "::Done" + "_" + getIndex());
        if (!done.isPresent()) {
            return false;
        } else {
            return Boolean.valueOf(done.get().getValue());
        }
    }

}
