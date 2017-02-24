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

import com.identiv.apduengine.ApduSession;
import com.identiv.apduengine.engine.ApduEnginePack;
import com.identiv.apduengine.engine.ApduResponse;

import java.util.List;

/**
 */
public interface ApduCommand {

    void setup(ApduSession session, int cmdIdx);

    /**
     * Allow command to clean up after failure (e.g. remove stuff from session
     * to allow retry etc). This is not guaranteed to be called.
     */
    void reset(ApduSession session);

    /**
     * Returns next set of APDUs to send (if any) or absent.
     */
    List<ApduEnginePack> getNextApduPacks();

    String getName();

    /**
     * Throws on failure
     */
    void validateResponse(ApduResponse response);

    boolean mustValidate();

    int getIndex();

    void setIndex(int index);
}
