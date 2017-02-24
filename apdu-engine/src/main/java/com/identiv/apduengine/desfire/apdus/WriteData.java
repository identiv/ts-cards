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

public class WriteData extends DesfireApduCommand {

    Logger logger = LoggerFactory.getLogger(WriteData.class);

    static final String DATA =
            WriteData.class.getSimpleName() + "::Data";

    public WriteData() {
    }

    public WriteData(byte[] data) {
        registerParameter(() -> new ApduSessionParameter(
                DATA, data
            ));
    }

    @Override
    public String getName() {
        return "Write Data";
    }

    @Override
    protected List<DesfireCommand> getDesfireCommands() {
        byte[] data = getSession()
                .getRequiredParameter(DATA, getIndex())
                .getValueAsBytes();
        byte[] lens = DesfireUtils.fileLenToBytes(data.length);
        byte[] apduData =
                Bytes.concat(
                        new byte[] { 0x01, 0x00, 0x00, 0x00 },
                        lens);

        byte[] crcData = new byte[8 + data.length];
        crcData[0] = (byte) 0x3D;
        System.arraycopy(apduData, 0, crcData, 1, apduData.length);
        System.arraycopy(data, 0, crcData, 8, data.length);
        byte[] crc32 = DesfireUtils.calCrc32(crcData);

        logger.info("CRC Data: {}", Hex.encode(crcData));
        logger.info("Crc32 = {}", Hex.encode(crc32));
        logger.info("Data payload: {}", Hex.encode(data));
        logger.info("Payload length bytes: {}", Hex.encode(lens));

        int len = data.length + 4;
        if (len % 16 > 0) {
            len += (16 - len % 16);
        }
        byte[] cryptogram = new byte[len];
        System.arraycopy(data, 0, cryptogram, 0, data.length);
        System.arraycopy(crc32, 0, cryptogram, data.length, crc32.length);
        byte[] cryptEnc = DesfireUtils.encrypt(getSession(), getIndex(), cryptogram,
                getSession().getRequiredParameter(DesfireAuth.SESSION_KEY).getValueAsBytes());

        byte[] commandData = new byte[apduData.length + cryptEnc.length];
        System.arraycopy(apduData, 0, commandData, 0, apduData.length);
        System.arraycopy(cryptEnc, 0, commandData, apduData.length, cryptEnc.length);
        return Lists.newArrayList(
                new DesfireCommand(0x3D, commandData)
        );
    }

    @Override
    protected boolean calculateCMAC(CmacOptions cmacOptions) {
        return cmacOptions == CmacOptions.RECEIVING;
    }

}
