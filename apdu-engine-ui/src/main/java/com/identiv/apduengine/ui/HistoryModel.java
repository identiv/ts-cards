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
package com.identiv.apduengine.ui;

import javafx.beans.property.SimpleStringProperty;

/**
 */
public class HistoryModel {
    private final SimpleStringProperty status;
    private final SimpleStringProperty command;
    private final SimpleStringProperty uid;
    private final SimpleStringProperty receivedData;

    public HistoryModel(String status, String command, String uid, String receivedData) {
        this.status = new SimpleStringProperty(status);
        this.command = new SimpleStringProperty(command);
        this.uid = new SimpleStringProperty(uid);
        this.receivedData = new SimpleStringProperty(receivedData);
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public String getCommand() {
        return command.get();
    }

    public void setCommand(String command) {
        this.command.set(command);
    }

    public String getReceivedData() {
        return receivedData.get();
    }

    public void setReceivedData(String receivedData) {
        this.receivedData.set(receivedData);
    }

    public String getUid() {
        return uid.get();
    }

    public void setUid(String command) {
        this.command.set(command);
    }
}
