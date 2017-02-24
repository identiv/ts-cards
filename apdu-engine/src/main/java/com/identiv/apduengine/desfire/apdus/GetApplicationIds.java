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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.engine.ApduResponse;
import com.identiv.apduengine.engine.util.Hex;

import java.util.*;

public class GetApplicationIds extends DesfireApduCommand {

    static final String AIDS_KEY =
            GetApplicationIds.class.getSimpleName() + "::Aids";

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        return Lists.newArrayList(
                new DesfireCommand(0x6a, EMPTY_DATA));
    }

    @Override
    public void validateResponse(ApduResponse response) {
        super.validateResponse(response);

        if (response.getStatus() != 0x9100) {
            throw new RuntimeException("Error response (" +
                    Integer.toHexString(response.getStatus()) + ")");
        } else {

            List<String> aids = new ArrayList<>();

            int macSize = !DesfireAuth.isAuthenticated(getSession()) ? 0 : 8;

            for (int i = 0; i < response.getData().length - macSize; i += 3 ) {
                byte[] aid = Arrays.copyOfRange(response.getData(),
                        i, i + 3);
                aids.add(Hex.encode(aid));
            }

            if (aids.size() > 0) {
                String aidsAsString = Joiner.on(';').join(aids);
                getSession().setParameter(
                        AIDS_KEY + "_" + getIndex(),
                        aidsAsString);
            }

            done();
        }
    }

    public List<String> getApplicationIds() {
        if (!isDone()) {
            return Collections.EMPTY_LIST;
        } else {
            Optional<ApduSessionParameter> aids = getSession().getParameter(
                    AIDS_KEY + "_" + getIndex());
            if (!aids.isPresent()) {
                return Collections.EMPTY_LIST;
            } else {
                Iterable<String> aidIterable = Splitter.on(';')
                        .split(aids.get().getValue());
                return Lists.newArrayList(aidIterable);
            }
        }
    }

}
