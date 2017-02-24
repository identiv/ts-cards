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
package com.identiv.apduengine.engine;

import java.io.Serializable;

/**
 * This represents a series of APDU commands and responses.
 */
public class ApduEnginePack implements Serializable {

    String apduName;
    byte[] command;
    StatusWord sw;

    public ApduEnginePack() {
    }

    public ApduEnginePack(String apduName, byte[] command, StatusWord sw) {
        this.apduName = apduName;
        this.command = command;
        this.sw = sw;
    }

    public ApduEnginePack(String apduName, byte[] command) {
        this.apduName = apduName;
        this.command = command;
        this.sw = StatusWord.DEFAULT_SUCCESS;
    }

    public String getApduName() {
        return apduName;
    }

    public void setApduName(String apduName) {
        this.apduName = apduName;
    }

    public byte[] getCommand() {
        return command;
    }

    public void setCommand(byte[] command) {
        this.command = command;
    }

    public StatusWord getSw() {
        return sw;
    }

    public void setSw(StatusWord sw) {
        this.sw = sw;
    }
}
