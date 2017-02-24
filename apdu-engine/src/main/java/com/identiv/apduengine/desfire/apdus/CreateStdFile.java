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
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.engine.util.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CreateStdFile extends DesfireApduCommand {

    Logger logger = LoggerFactory.getLogger(CreateStdFile.class);

    public static final String FILE_LENGTH =
            CreateStdFile.class.getSimpleName() + "::FileLength";

    public CreateStdFile() {
    }

    public CreateStdFile(int len) {
        registerParameter(() -> new ApduSessionParameter(
                FILE_LENGTH, String.valueOf(len)
            ));
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        String length = getSession().
                getRequiredParameter(FILE_LENGTH, getIndex()).
                getValue();
        byte[] lens = DesfireUtils.fileLenToBytes(Integer.valueOf(length));
        logger.debug("Data length: {}/{}", length, Hex.encode(lens));
        return Lists.newArrayList(
                new DesfireCommand(0xCD,
                        Bytes.concat(
                            new byte[] { 0x01, 0x03, 0x00, 0x10 },
                            lens
                        )));
    }

}
