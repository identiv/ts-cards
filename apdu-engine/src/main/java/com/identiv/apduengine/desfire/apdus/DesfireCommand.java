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

import com.identiv.apduengine.engine.StatusWord;

/**
 */
public class DesfireCommand {
    private Integer commandIns;
    private byte[] data;
    private StatusWord statusWord;

    public DesfireCommand(Integer commandIns, byte[] data) {
        this(commandIns, data, 0xFFFF, 0x9100);
    }

    public DesfireCommand(Integer commandIns, byte[] data,
                          int expectedStatusMask, int expectedStatus) {
        this(commandIns, data,
                new StatusWord(expectedStatusMask, expectedStatus));

    }

    public DesfireCommand(Integer commandIns, byte[] data,
                          StatusWord statusWord) {
        this.commandIns = commandIns;
        this.data = data;
        this.statusWord = statusWord;
    }


    public Integer getCommandIns() {
        return commandIns;
    }

    public void setCommandIns(Integer commandIns) {
        this.commandIns = commandIns;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public StatusWord getStatusWord() {
        return statusWord;
    }

    public void setStatusWord(StatusWord statusWord) {
        this.statusWord = statusWord;
    }

}
