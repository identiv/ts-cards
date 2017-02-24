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
package com.identiv.apduengine.apdus;

import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.engine.ApduResponse;

import javax.smartcardio.CommandAPDU;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 */
public abstract class AbstractApduCommand implements ApduCommand {

    private ApduSession session;
    private int index = -1;
    private List<Supplier<ApduSessionParameter>> parameters = new LinkedList<>();

    // allow parameters to put into the session at a later time
    protected void registerParameter(Supplier<ApduSessionParameter>... parameters) {
        for (int i = 0; i < parameters.length; i++) {
            this.parameters.add(parameters[i]);
        }
    }

    public ApduSession getSession() {
        return session;
    }

    /**
     * Saves the session object for later use; sets registerParameters into the
     * session.
     */
    @Override
    public void setup(ApduSession session, int index) {
        this.session = session;
        this.index = index;
        parameters.forEach((supplier) -> {
            // this allows some default/initial values to be set, existing
            // values will not replaced here
            ApduSessionParameter parameter = supplier.get();
            parameter.setName(parameter.getName() + "_" + index);
            if (!session.getParameters().containsKey(parameter.getName())) {
                session.setParameter(parameter);
            } else {
                session.getRequiredParameter(parameter.getName()).setValue(
                        parameter.getValue());
            }
        });
        parameters.clear();
    }

    @Override
    public void validateResponse(ApduResponse response) {
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public boolean mustValidate() {
        return false;
    }

    @Override
    public void reset(ApduSession session) {}

    public static byte[] withZeroLe(CommandAPDU command) {

        byte[] commandBytes = command.getBytes();
        byte[] withResponseLength = new byte[commandBytes.length + 1];
        System.arraycopy(commandBytes, 0,
                withResponseLength, 0, commandBytes.length);

        return withResponseLength;
    }

    @Override
    public boolean equals(Object o) {
        return getClass().equals(o.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

}
