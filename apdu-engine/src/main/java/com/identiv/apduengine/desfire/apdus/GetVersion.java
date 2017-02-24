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

import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.engine.ApduResponse;

import java.util.ArrayList;
import java.util.List;


/**
 */
public class GetVersion extends DesfireApduCommand {

    public static final String STAGE_KEY =
            GetVersion.class.getSimpleName() + "::Stage";

    enum Stage {
        FIRST, SECOND, THIRD
    }

    @Override
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return false;
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {

        List<DesfireCommand> commands = new ArrayList<>();

        if (!isDone()) {
            if (!DesfireAuth.isAuthenticated(getSession())) {
                commands.add(new DesfireCommand(0x60, EMPTY_DATA, 0xFFFF, 0x91AF));
                commands.add(new DesfireCommand(0xAF, EMPTY_DATA, 0xFFFF, 0x91AF));
                commands.add(new DesfireCommand(0xAF, EMPTY_DATA, 0xFFFF, 0x9100));
                done();
            } else {
                ApduSessionParameter stage = getSession().getParameterOrNew(
                        STAGE_KEY, Stage.FIRST.name());
                switch (Stage.valueOf(stage.getValue())) {
                    case FIRST:
                        commands.add(new DesfireCommand(0x60, EMPTY_DATA, 0xFFFF, 0x91AF));
                        break;
                    case SECOND:
                        commands.add(new DesfireCommand(0xAF, EMPTY_DATA, 0xFFFF, 0x91AF));
                        break;
                    case THIRD:
                        commands.add(new DesfireCommand(0xAF, EMPTY_DATA, 0xFFFF, 0x9100));
                        break;
                }
            }
        }

        return commands;

    }

    @Override
    public void validateResponse(ApduResponse response) {
        if (isDone()) {
            byte[] uid = new byte[7];
            System.arraycopy(response.getData(), 0, uid, 0, uid.length);
            getSession().setParameter(DesfireGetCardUID.CARD_UID_KEY,
                    uid);
            super.validateResponse(response);
        } else {
            super.validateResponse(response);

            ApduSessionParameter stage = getSession().getParameterOrNew(
                    STAGE_KEY, Stage.FIRST.name());
            if (Stage.valueOf(stage.getValue()) != Stage.THIRD) {
                undone();
            }

            switch (Stage.valueOf(stage.getValue())) {
                case FIRST:
                    stage.setValue(Stage.SECOND.name());
                    break;
                case THIRD:
                case SECOND:
                    stage.setValue(Stage.THIRD.name());
                    break;
            }
        }
    }

    @Override
    public boolean mustValidate() {
        return true;
    }

    @Override
    public String getName() {
        return "Get Version";
    }
}
