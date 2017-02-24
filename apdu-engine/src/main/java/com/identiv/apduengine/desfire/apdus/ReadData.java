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
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReadData extends DesfireApduCommand {

    Logger logger = LoggerFactory.getLogger(ReadData.class);

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        return Lists.newArrayList(
                new DesfireCommand(0xBD, new byte[] {
                        0x01, // file number
                        0x00, 0x00, 0x00, // offset
                        0x00, 0x00, 0x00 }) // data to read
        );
    }

    @Override
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return cmacOptions == CmacOptions.SENDING;
    }

    @Override
    public void validateResponse(ApduResponse response) {
        // This is kind of interesting. For the Read Data command we do
        // need to calculate the CMAC for sending the command but *not* for the
        // response.

        if (response.getStatus() == 0x9100) {
            byte[] sessionKey = getSession()
                    .getRequiredParameter(DesfireAuth.SESSION_KEY)
                    .getValueAsBytes();
            byte[] raw = DesfireUtils.decrypt(getSession(), getIndex(),
                    response.getData(), sessionKey);
            logger.info("Read data -> {}", Hex.encode(raw));
        }
        super.validateResponse(response);
    }

    @Override
    public String getName() {
        return "Read Data";
    }
}
