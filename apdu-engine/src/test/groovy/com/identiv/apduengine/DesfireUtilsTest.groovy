/*
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
package com.identiv.apduengine;

import com.google.common.primitives.Bytes;
import com.identiv.apduengine.desfire.DesfireUtils
import com.identiv.apduengine.desfire.apdus.DesfireKeyType
import com.identiv.apduengine.desfire.crypto.JceAesCryptor
import com.identiv.apduengine.engine.util.Hex
import spock.lang.Shared
import spock.lang.Specification

/**
 */
class DesfireUtilsTest extends Specification {

    void "assemble session keys from inputs"(DesfireKeyType keyType,
                                             String rndA, String rndB,
                                             String result) {
        when:
            byte[] sessionKey = DesfireUtils.makeSessionKey(
                    keyType, Hex.decode(rndA), Hex.decode(rndB))
        then:
            Hex.encode(sessionKey) == result
        where:
            keyType                     |  rndA | rndB || result
            DesfireKeyType.AES_128      | "6CAB6B696A297CAEF41F4DCC32EDB102" |
                                          "7CE60F1A7E21897EBC997A958A2F73AD" ||
                                          "6CAB6B697CE60F1A32EDB1028A2F73AD"
            DesfireKeyType.AES_128      | "E11E8EE135A941DD833853C7DD1D7F96" |
                                          "4F65D607A629EE0C98C3B19245B502AA" ||
                                          "E11E8EE14F65D607DD1D7F9645B502AA"
            DesfireKeyType.AES_128      | "8E334447AF8F11D40174B29ABA21A125" |
                                          "540E20415B9F721A6ECA83DD7B39C49C" ||
                                          "8E334447540E2041BA21A1257B39C49C"
            DesfireKeyType.TWO_KEY_3DES | "3CE2A1BACEE6C5F2" |
                                          "1B47BCAFC9A02981" ||
                             "3CE2A1BA1B47BCAF3CE2A1BA1B47BCAF3CE2A1BA1B47BCAF"
            DesfireKeyType.TWO_KEY_3DES | "E09D9F27699CC00E" |
                                          "C189F69D82454312" ||
                             "E09D9F27C189F69DE09D9F27C189F69DE09D9F27C189F69D"

    }

    @Shared
    byte[] key = Hex.decode("2b7e1516 28aed2a6 abf71588 09cf4f3c")

    void "cmac examples from NIST SP800-38B"(String message,
                                             String result) {
        when:
            byte[] cmac = DesfireUtils.cmac(Hex.decode(message),
                    new JceAesCryptor(key), new byte[16], true);
        then:
            Hex.encode(cmac) == result.toUpperCase()
        where:
            message                               || result
            ""                                    ||
                                             "bb1d6929e95937287fa37d129b756746"
            "6bc1bee2 2e409f96 e93d7e11 7393172a" ||
                                             "070a16b46b4d4144f79bdd9dd04a287c"
            "6bc1bee2 2e409f96 e93d7e11 7393172a" +
                    " ae2d8a57 1e03ac9c 9eb76fac" +
                    " 45af8e51 30c81c46 a35ce411" ||
                                             "dfa66747de9ae63030ca32611497c827"


    }

    void "key diversification per AN10922"(String divData,
                                           String masterKey,
                                           String result) {
        when:
            byte[] cmac = DesfireUtils.cmacKeyDivAes128(Hex.decode(divData),
                    new JceAesCryptor(Hex.decode(masterKey)));
        then:
            Hex.encode(cmac) == result.toUpperCase()
        where:
            divData                              | masterKey                          || result
            // from AN10922
            "04782E21801D803042F54E585020416275" | "00112233445566778899AABBCCDDEEFF" || "A8DD63A3B89D54B37CA802473FDA9175"
            // https://github.com/RedFroggy/symmetric-key-derivation/blob/master/src/test/java/fr/redfroggy/sample/derivation/services/DiversificationStandardTest.java
            "11223344556677CCBBAA010000000000000000000000000000000000000000" |
                                                 "00112233445566778899AABBCCDDEEFF"   || "DB29A5E17D9414DE4BE5C0B10B49A1D2"

    }


    void "file size calculate"(int length, String result) {
        when:
            byte[] calc = DesfireUtils.fileLenToBytes(length)
        then:
            Hex.encode(calc) == result.toUpperCase()
        where:
            length || result
            3      || "030000"
            32     || "200000"

    }

    void "crc32 calculate"(byte[] pageload, String result) {
        when:
            byte[] lens = DesfireUtils.fileLenToBytes(pageload.length);
            byte[] apduData =
                    Bytes.concat(
                            (byte[]) [ 1, 0, 0, 0 ],
                            lens)
            byte[] crcData = new byte[8 + pageload.length]
            crcData[0] = (byte) 0x3D;
            System.arraycopy(apduData, 0, crcData, 1, apduData.length)
            System.arraycopy(pageload, 0, crcData, 8, pageload.length)
            byte[] crc32 = DesfireUtils.calCrc32(crcData)
        then:
            Hex.encode(crc32) == result.toUpperCase()
        where:
            pageload                            || result
            Hex.decode("0A08220625C0513DDF98")  || "287428C8"
            Hex.decode("0A0822062502479D9970")  || "791805FF"
            Hex.decode("0A082206258234392E80")  || "469821FF"
            Hex.decode("0A082206250000000F98")  || "E84707FF"
    }
}
