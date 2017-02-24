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
package com.identiv.apduengine.engine.util;

public final class Hex {

    private static char[] hexDigit = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    public static String encode(byte[] data) {
        StringBuilder hexString = new StringBuilder();
        for (byte b: data) {
            int actualByte = b & 0xff;
            char bh = hexDigit[(actualByte >>> 4)];
            char bl = hexDigit[(actualByte & 0xF)];
            hexString.append(bh).append(bl);
        }

        return hexString.toString();
    }

    public static byte[] decode(String hexString) {
        if (hexString == null) {
            throw new IllegalArgumentException("The input must be not null.");
        } else if (hexString.isEmpty()) {
            return new byte[0];
        } else {
            hexString = hexString.replace(" ", "");
            hexString = hexString.toUpperCase();
            byte[] ret = new byte[hexString.length() / 2];

            int j = 0;
            for (int i = 0; i < hexString.length(); i += 2) {
                byte b1= (byte) (hexString.charAt(i)   -'0'); if (b1>9) b1 -= 7;
                byte b2= (byte) (hexString.charAt(i + 1) -'0'); if (b2>9) b2 -= 7;
                ret[j++] = (byte) ((b1<<4) + b2);
            }

            return ret;
        }
    }
}
