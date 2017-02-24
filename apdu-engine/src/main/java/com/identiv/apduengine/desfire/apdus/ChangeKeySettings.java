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
import com.identiv.apduengine.ApduSessionParameter;
import com.identiv.apduengine.desfire.DesfireUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 */
public class ChangeKeySettings extends DesfireApduCommand {

    Logger logger = LoggerFactory.getLogger(ChangeKeySettings.class);

    static final String SETTINGS =
            ChangeKeySettings.class.getSimpleName() + "::Settings";

    public ChangeKeySettings() {
    }

    public ChangeKeySettings(byte settings) {
        registerParameter(() -> new ApduSessionParameter(
                SETTINGS, new byte[] { settings })
        );
    }

    @Override
    public String getName() {
        return "Change Key Settings";
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {

        byte newSettings = getSession()
                .getRequiredParameter(SETTINGS, getIndex())
                .getValueAsBytes()[0];

        byte[] crcData = new byte[2];
        crcData[0] = 0x54; // instruction
        crcData[1] = newSettings;
        byte[] crc32 = DesfireUtils.calCrc32(crcData);

        DesfireKeyType sessionType = DesfireKeyType.valueOf(getSession()
                .getKeyTypeParameter(true, getIndex())
                .getValue());

        byte[] plain = new byte[sessionType.blockSize];
        plain[0] = newSettings;
        System.arraycopy(crc32, 0, plain, 1, crc32.length);

        byte[] sessionKey = getSession()
                .getRequiredParameter(DesfireAuth.SESSION_KEY)
                .getValueAsBytes();
        byte[] crypted = DesfireUtils.encrypt(getSession(), getIndex(),
                plain, sessionKey);

        return Lists.newArrayList(
                new DesfireCommand(0x54, crypted)
        );

    }

    @Override
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return CmacOptions.RECEIVING == cmacOptions;
    }

}
