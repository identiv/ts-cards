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
package com.identiv.apduengine.engine;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.identiv.apduengine.engine.ApduStatus.INCOMPLETE;
import static com.identiv.apduengine.engine.ApduStatus.SUCCESS;

/**
 */
public class ApduEngine implements Serializable {

    transient Logger logger = LoggerFactory.getLogger(ApduEngine.class);

    List<ApduEnginePack> apduPacks;

    ApduStatus status;

    public ApduEngine() {}


    public void initialize(List<ApduEnginePack> packs) {
        this.status = SUCCESS;

        this.apduPacks = new ArrayList<>(packs);
    }

    /**
     * Returns next set of APDU bytes or an empty Optional if there is nothing
     * else to do.
     *
     * @return
     */
    public Optional<ApduEnginePack> nextApdu() {
        if (status == ApduStatus.FAILURE) {
            throw new RuntimeException("In failure state, re-initialize session");
        } else {
            if (apduPacks.isEmpty()) {
                return Optional.absent(); // all done
            } else {
                ApduEnginePack next = apduPacks.get(0);
                apduPacks.remove(0);
                return Optional.of(next);
            }
        }
    }

    public ApduStatus validateResponse(ApduEnginePack pack, ApduResponse response) {

        StatusWord sw = pack.getSw();

        if ((sw.getMask() & response.getStatus()) != sw.getExpectedSw()) {
            return ApduStatus.FAILURE;
        }
        return SUCCESS;
    }

    ApduResponse transmit(ApduTransmitter transmitter, ApduEnginePack pack) {

        logger.info("{} --send--> [{}] (size: {})",
                new Object[] {
                        pack.getApduName(),
                        Hex.encode(pack.getCommand()),
                        pack.getCommand().length
                });

        ListenableFuture<ApduResponse> futureResponse = transmitter.apply(pack.getCommand());

        try {
            ApduResponse response = futureResponse.get();

            logger.info("{} <--recv-- [{}]/{} (size: {})",
                    new Object[] {
                            pack.getApduName(),
                            Hex.encode(response.getData()),
                            Integer.toHexString(response.getStatus()),
                            response.getData().length
                    });

            return response;

        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }

    }

    StatusResponse transmit(ApduTransmitter transmitter) {
        Optional<ApduEnginePack> apdu = nextApdu();
        if (!apdu.isPresent()) {
            // either we're done (or an error of some kind)
            return new StatusResponse(SUCCESS);
        } else {

            ApduResponse apduResponse = transmit(transmitter, apdu.get());

            ApduStatus status = validateResponse(apdu.get(), apduResponse);

            // there are probably more elegant ways to do this
            if (status == SUCCESS && !apduPacks.isEmpty()) {
                status = INCOMPLETE;
            }

            return new StatusResponse(status, apduResponse);
        }
    }

    public StatusResponse sendSequence(ApduTransmitter transmitter) {

        StatusResponse statusResponse;
        do {
            statusResponse = transmit(transmitter);
            logger.debug("Status is: {}", statusResponse.getStatus());
        } while (statusResponse.getStatus() == INCOMPLETE);

        return statusResponse;
    }


    public ApduStatus getStatus() {
        return status;
    }

    public void setStatus(ApduStatus status) {
        this.status = status;
    }

    public List<ApduEnginePack> getApduPacks() {
        return this.apduPacks;
    }
}
