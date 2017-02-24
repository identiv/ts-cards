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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.identiv.apduengine.apdus.ApduCommand;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.desfire.apdus.DesfireGetCardUID;
import com.identiv.apduengine.engine.*;
import com.identiv.apduengine.engine.util.Hex;

import java.util.*;


/**
 */
public class ApduSession {

    List<ApduCommand> apduCommands = new ArrayList<>();
    int index;
    Map<String, ApduSessionParameter> parameters = new HashMap<>();


    public ApduSession() {
    }

    public ApduSession(ApduCommand... apduCommands) {
        Collections.addAll(this.apduCommands, apduCommands);
        this.index = 0;
        for (int i=0; i<this.apduCommands.size(); i++) {
            this.apduCommands.get(i).setup(this, i);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApduSession that = (ApduSession) o;

        if (index != that.index) return false;
        if (apduCommands != null ? !apduCommands.equals(that.apduCommands) : that.apduCommands != null) {
            return false;
        }
        return parameters != null ? parameters.equals(that.parameters) : that.parameters == null;

    }

    @Override
    public int hashCode() {
        int result = apduCommands != null ? apduCommands.hashCode() : 0;
        result = 31 * result + index;
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    /**
     * Discard existing command queue and replace with specified commands.
     * Unlike reset() this will maintain the session state.
     */
    public void nextCommands(ApduCommand... apduCommands) {
        resetParametersForNextCommands();

        this.apduCommands.clear();
        Collections.addAll(this.apduCommands, apduCommands);
        this.index = 0;
        for (int i=0; i<this.apduCommands.size(); i++) {
            this.apduCommands.get(i).setup(this, i);
        }
    }

    // remove all except KEY_TYPE_KEY, INIT_VEC_KEY, SESSION_KEY, STAGE_KEY and CARD_UID_KEY
    private void resetParametersForNextCommands() {
        ApduSessionParameter keyTypeParam = getKeyTypeParameter(
                false, this.apduCommands.size() - 1);

        Map<String, ApduSessionParameter> tempParams = new HashMap<>();
        List<String> keepKeys = Lists.newArrayList(
                DesfireAuth.INIT_VEC_KEY,
                DesfireAuth.SESSION_KEY,
                DesfireGetCardUID.CARD_UID_KEY,
                DesfireAuth.STAGE_KEY
        );
        for (String key : keepKeys) {
            if (this.parameters.containsKey(key)) {
                tempParams.put(key, this.parameters.get(key));
            }
        }

        this.parameters.clear();
        this.parameters.putAll(tempParams);

        if (keyTypeParam != null) {
            setParameter(DesfireAuth.KEY_TYPE_KEY, keyTypeParam.getValue());
        }
    }

    /**
     * Reset object with new commands (discards session).
     */
    public void reset(ApduCommand... apduCommands) {
        this.apduCommands.forEach(apduCommand -> {
            apduCommand.reset(this);
        });
        this.apduCommands.clear();
        Collections.addAll(this.apduCommands, apduCommands);
        this.index = 0;
    }

    public ApduStatus validateLast(ApduResponse response) {
        apduCommands.get(index).validateResponse(response);
        return ApduStatus.SUCCESS;
    }

    public StatusResponse transmit(ApduTransmitter transmitter) {
        Optional<ApduEngine> apduEngine = nextApduEngine();

        ApduStatus status = ApduStatus.SUCCESS;
        StatusResponse response = null;

        while (apduEngine.isPresent()) {
            response = apduEngine.get().sendSequence(transmitter);
            status = response.getStatus();
            if (status == ApduStatus.FAILURE) {
                break;
            }
            apduEngine = nextApduEngine(response.getResponse().orNull());
        }

        return response == null ? new StatusResponse(status, null) :
                response;
    }

    public Optional<ApduEngine> nextApduEngine() {
        return nextApduEngine(null);
    }

    public Optional<ApduEngine> nextApduEngine(ApduResponse response) {

        if (index >= apduCommands.size()) {
            return Optional.empty();
        }
        if (response != null) {
            ApduCommand currentCmd = apduCommands.get(index);
            currentCmd.setup(this, index);
            currentCmd.validateResponse(response);
        }

        ApduEngine engine = new ApduEngine();

        List<ApduEnginePack> packs = new ArrayList<>();

        for (; index < apduCommands.size(); index++) {
            ApduCommand thisCommand = apduCommands.get(index);
            thisCommand.setup(this, index);
            List<ApduEnginePack> nextPacks = thisCommand.getNextApduPacks();
            if (!nextPacks.isEmpty()) {
                packs.addAll(nextPacks);
                if (thisCommand.mustValidate()) {
                    // then we stop here but continue when requested
                    break;
                }
            }
        }

        if (packs.size() == 0) {
            return Optional.empty();
        } else {
            engine.initialize(packs);
            return Optional.of(engine);
        }
    }

    public List<ApduCommand> getApduCommands() {
        return apduCommands;
    }

    public void setApduCommands(List<ApduCommand> apduCommands) {
        this.apduCommands = apduCommands;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Map<String, ApduSessionParameter> getParameters() {
        return parameters;
    }

    public void setParameter(ApduSessionParameter parameter) {
        parameters.put(parameter.getName(), parameter);
    }

    public void setParameter(String name, String value) {
        setParameter(new ApduSessionParameter(name, value));
    }

    public void setParameter(String name, byte[] value) {
        setParameter(new ApduSessionParameter(name, value));
    }

    public Optional<ApduSessionParameter> getParameter(String name) {
        return Optional.ofNullable(parameters.get(name));
    }

    public ApduSessionParameter getRequiredParameter(String name) {
        ApduSessionParameter parameter = parameters.get(name);
        Preconditions.checkNotNull(parameter,
                "Parameter " + name + " is required but not present in session.");
        return parameter;
    }

    public ApduSessionParameter getRequiredParameter(String name, int comIdx) {
        name = name + "_" + comIdx;
        return getRequiredParameter(name);
    }

    public ApduSessionParameter getParameterOrNew(String name, String value) {
        if (!parameters.containsKey(name)) {
            setParameter(name, value);
        }
        return parameters.get(name);
    }

    public ApduSessionParameter getParameterOrNew(String name, byte[] value) {
        if (!parameters.containsKey(name)) {
            setParameter(name, Hex.encode(value));
        }
        return parameters.get(name);
    }

    public void removeParameter(String name) {
        parameters.remove(name);
    }

    public void removeParameter(String name, int index) {
        name = name + "_" + index;
        removeParameter(name);
    }

    public ApduSessionParameter getKeyTypeParameter(boolean required, int currentIndex) {
        ApduSessionParameter parameter = null;
        for (int idx = currentIndex; idx >= 0; idx--) {
            if (apduCommands.get(idx) instanceof DesfireAuth) {
                String name = DesfireAuth.KEY_TYPE_KEY + "_" + idx;
                parameter = parameters.get(name);
                break;
            }
        }
        if (parameter == null) {
            parameter = parameters.get(DesfireAuth.KEY_TYPE_KEY);
        }
        if (required) {
            Preconditions.checkNotNull(parameter,
                    "Parameter " + DesfireAuth.KEY_TYPE_KEY + " is required but not present in session.");
        }
        return parameter;
    }
}
