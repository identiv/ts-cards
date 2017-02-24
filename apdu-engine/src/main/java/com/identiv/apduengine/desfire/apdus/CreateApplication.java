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

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import com.identiv.apduengine.ApduSessionParameter;

import java.util.List;

public class CreateApplication extends DesfireApduCommand {

    public static final String AID_KEY =
            CreateApplication.class.getSimpleName() + "::Data";

    public CreateApplication() {
    }

    public CreateApplication(byte[] aid) {
        registerParameter(() -> new ApduSessionParameter(
                AID_KEY, aid
        ));
    }

    @Override
    public String getName() {
        return "Create Application";
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        byte[] aid = getSession()
                .getRequiredParameter(AID_KEY, getIndex())
                .getValueAsBytes();
        return Lists.newArrayList(
                new DesfireCommand(0xCA,
                        Bytes.concat(aid,
                        new byte[] { 0x0B, (byte) 0x82 })));
    }

}
