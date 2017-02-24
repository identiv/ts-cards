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

/**
 * Creates the "TS Application" - for Identiv this is a container for the OACF
 * data. Some care should be taken here if you are issueing/programming
 * non-Identiv cards (considering using a different AID).
 */
public class CreateTsApplication extends CreateApplication {

    static final byte[] TS_AID = new byte[] { 0x53, 0x54, (byte) 0xFF };

    public CreateTsApplication() {
        super(TS_AID);
    }

}
