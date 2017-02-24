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
 */
public enum DesfireKeyType {
    TWO_KEY_3DES(24, 8, 8),
    AES_128(16, 16, 16);

    final int challengeSize;
    final int keySize;
    final int blockSize;

    DesfireKeyType(int keySize, int challengeSize, int blockSize) {
        this.keySize = keySize;
        this.challengeSize = challengeSize;
        this.blockSize = blockSize;
    }

    public int getChallengeSize() {
        return challengeSize;
    }

    public int getKeySize() {
        return keySize;
    }

    public int getBlockSize() {
        return blockSize;
    }
}
