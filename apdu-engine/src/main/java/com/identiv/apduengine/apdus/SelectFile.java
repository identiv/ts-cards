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

import com.identiv.apduengine.engine.ApduEnginePack;

import javax.smartcardio.CommandAPDU;

/**
 */
public class SelectFile extends SimpleApduCommand {

    public static final byte[] DESFIRE_APPLICATION = new byte[] {
            (byte) 0xd2, 0x76, 0x00, 0x00,
            (byte) 0x85, 0x01, 0x00
    };


    public static final byte[] MASTER_FILE = new byte[] {
            0x3f, 0x00
    };

    private byte[] aid;

    public SelectFile() {
        this.aid = MASTER_FILE;
    }

    public SelectFile(byte[] aid) {
        this.aid = aid;
    }

    @Override
    protected ApduEnginePack getSingleApduPack() {
        CommandAPDU command =
                new CommandAPDU(
                        0x00,                   // CLA
                        0xa4,                   // INS
                        0x00,                   // P1
                        0x00,                   // P2
                        aid);

        return new ApduEnginePack(getName(), withZeroLe(command));
    }

}
