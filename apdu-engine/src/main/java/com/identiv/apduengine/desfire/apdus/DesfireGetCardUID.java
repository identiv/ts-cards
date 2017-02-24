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
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.ApduResponse;

import java.util.List;

/**
 */
public class DesfireGetCardUID extends DesfireApduCommand {

    public static final String CARD_UID_KEY =
            DesfireGetCardUID.class.getSimpleName() + "::CardUid";

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        return Lists.newArrayList(
            new DesfireCommand(0x51, EMPTY_DATA)
        );
    }

    @Override
    public String getName() {
        return "Get DESFire UID";
    }

    @Override
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return cmacOptions == CmacOptions.SENDING;
    }

    @Override
    public void validateResponse(ApduResponse response) {
        byte[] uid = new byte[7];
        System.arraycopy(DesfireUtils.decrypt(getSession(), getIndex(), response.getData(),
                getSession().getRequiredParameter(DesfireAuth.SESSION_KEY).getValueAsBytes()),
                0, uid, 0, uid.length);
        getSession().setParameter(CARD_UID_KEY, uid);
        super.validateResponse(response);
    }
}
