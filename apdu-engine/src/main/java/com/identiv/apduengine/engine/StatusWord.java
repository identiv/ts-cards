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
public class StatusWord implements Serializable {

    public static final StatusWord DEFAULT_SUCCESS =
            new StatusWord(0xFFFF, 0x9000);

    private int mask;
    private int expectedSw;

    public StatusWord() {
    }

    public StatusWord(int mask, int expectedSw) {
        this.mask = mask;
        this.expectedSw = expectedSw;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getExpectedSw() {
        return expectedSw;
    }

    public void setExpectedSw(int expectedSw) {
        this.expectedSw = expectedSw;
    }
}
