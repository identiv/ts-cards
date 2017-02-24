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
package com.identiv.apduengine;

import com.google.common.primitives.Bytes;
import com.identiv.apduengine.desfire.DesfireUtils;
import com.identiv.apduengine.desfire.apdus.DesfireAuth;
import com.identiv.apduengine.engine.util.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 */
public class ProtectedCommunicationTest {

    @Test
    public void sampleDocs() {

//        *** GetKeyVersion()
//        TX CMAC:  25 7F C5 38 61 8A 94 4A 3A 20 96 7B 6F 31 43 48
//        Sending:  00 00 FF 05 FB <D4 40 01 64 00> 87 00
//        Response: 00 00 FF 0D F3 <D5 41 00 00 10 8A 8F A3 6F 55 CD 21 0D> 5F 00
//        RX CMAC:  8A 8F A3 6F 55 CD 21 0D D8 05 46 58 AC 70 D9 9A
//        Version: 0x10

        String formattedExpected = "8A 8F A3 6F 55 CD 21 0D D8 05 46 58 AC 70 D9 9A";
        String expected = Hex.encode(Hex.decode(formattedExpected));

        byte[] sent = Hex.decode("64 00");
        byte[] response = Hex.decode("10");
        ApduSession session = new ApduSession();
        session.setParameter(DesfireAuth.SESSION_KEY,
                Hex.decode("90 F7 A2 01 91 03 68 45 EC 63 DE CD 54 4B 99 31"));

        byte[] cmac1 = DesfireUtils.cmac(session, sent, true);
        byte[] cmac2 = DesfireUtils.cmac(session,
                Bytes.concat(response, new byte[1]), true);

        System.out.println("Expected RX CMAC: " + expected);
        System.out.println("      calculated: " + Hex.encode(cmac2));

        Assert.assertEquals(expected, Hex.encode(cmac2));

    }


    @Test
    public void fromObservedCardBehaviour() {

//        **** SessionKey : C4C003424E37543FA91D218AF8F43070 (AES_128)
//        **** InitVec : ApduSessionParameter {name='DesfireAuth::InitVec', value='ECD4236DBAB20293E009883BCD93966E'}
//        INFO: FormatCard --send--> [90FC000000] (size: 5)
//        INFO: FormatCard <--recv-- [DC438112CF631AE6]/9100 (size: 8)

        String expected = "DC438112CF631AE6";

        byte[] sent = Hex.decode("FC");
        byte[] response = Hex.decode("00");

        ApduSession session = new ApduSession();
        session.setParameter(DesfireAuth.SESSION_KEY,
                Hex.decode("C4C003424E37543FA91D218AF8F43070"));

        byte[] cmac1 = DesfireUtils.cmac(session, sent, true);
        byte[] cmac2 = DesfireUtils.cmac(session, response, true);

        Assert.assertEquals(expected,
                Hex.encode(Arrays.copyOfRange(cmac2, 0, cmac2.length/2)));

    }

    @Test
    public void fromObservedCardBehaviour2() {

//        **** SessionKey : 485F5D230B5B6297C06F376C9A96F779 (AES_128)
//        **** InitVec : ApduSessionParameter {name='DesfireAuth::InitVec', value='F90B6388A3D9610ED63246642C709ADA'}
//        INFO: Calculating CMAC from FC
//        WARNING: Calculated new CMAC and updating IV: 5B542A5D51C4B2944000D8F2FD3DBD88
//        INFO: FormatCard --send--> [90FC000000] (size: 5)
//        INFO: FormatCard <--recv-- [EFC64A6688BD665F]/9100 (size: 8)

        String expected = "EFC64A6688BD665F";

        byte[] sent = Hex.decode("FC");
        byte[] response = Hex.decode("00");

        ApduSession session = new ApduSession();
        session.setParameter(DesfireAuth.SESSION_KEY,
                Hex.decode("485F5D230B5B6297C06F376C9A96F779"));

        byte[] cmac1 = DesfireUtils.cmac(session, sent, true);
        byte[] cmac2 = DesfireUtils.cmac(session, response, true);

        Assert.assertEquals(expected,
                Hex.encode(Arrays.copyOfRange(cmac2, 0, cmac2.length/2)));

    }


}
