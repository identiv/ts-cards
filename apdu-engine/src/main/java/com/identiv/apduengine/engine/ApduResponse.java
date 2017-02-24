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
 */
public class ApduResponse implements Serializable {

    public static byte[] EMPTY_BODY = new byte[0];

    Integer status;
    byte[] data;

    public ApduResponse() {
    }

    public ApduResponse(Integer status, byte[] data) {
        this.status = status;
        if (data == null) {
            this.data = EMPTY_BODY;
        } else {
            this.data = data;
        }
    }

    public ApduResponse(byte[] data) {

        this.data = new byte[data.length - 2];
        System.arraycopy(data, 0, this.data, 0, data.length - 2);

        status = ((data[data.length - 2] << 8) & 0xff00) |
                 ((data[data.length - 1] << 0) & 0x00ff);

    }

    public ApduResponse(byte sw1, byte sw2) {
        this(sw1, sw2, EMPTY_BODY);
    }

    public ApduResponse(byte sw1, byte sw2, byte[] data) {
        status = ((sw1 << 8) & 0xff00) |
                 ((sw2 << 0) & 0x00ff);

        if (data == null) {
            this.data = EMPTY_BODY;
        } else {
            this.data = data;
        }
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
