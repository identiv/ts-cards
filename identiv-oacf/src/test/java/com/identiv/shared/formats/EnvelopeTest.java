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
package com.identiv.shared.formats;

import com.google.protobuf.ByteString;
import com.idondemand.client.utils.Hex;
import org.junit.Test;

/**
 */
public class EnvelopeTest {

    @Test
    public void formatDemo() {
        // 3535D (4050, 1000000)
        byte[] fixedBytes = Hex.decode("23FF4BD09000");

        OpenAccessCredentialFormat.PacsRecord pacsRecord =
                OpenAccessCredentialFormat.PacsRecord.newBuilder()
                        .setWiegandData(ByteString.copyFrom(fixedBytes))
                        .build();

        OpenAccessCredentialFormat.CredentialEnvelope envelope =
            OpenAccessCredentialFormat.CredentialEnvelope.newBuilder()
                .setDefaultPacsRecord(pacsRecord)
                .build();

        System.out.println(envelope.toString());
        System.out.println("************" + Hex.encode(fixedBytes));
        System.out.println(Hex.encode(envelope.toByteArray()));
    }

    @Test
    public void formatDemo37DNoChecksum() {
        // 37D (17265057273)
        byte[] fixedBytes = Hex.decode("25C0513DDF98");

        OpenAccessCredentialFormat.CredentialEnvelope envelope =
            OpenAccessCredentialFormat.CredentialEnvelope.newBuilder()
                .setDefaultPacsRecord(
                    OpenAccessCredentialFormat.PacsRecord.newBuilder()
                        .setWiegandData(ByteString.copyFrom(fixedBytes))
                        .build())
                .build();

        System.out.println(envelope.toString());
        System.out.println("************" + Hex.encode(fixedBytes));
        System.out.println(Hex.encode(envelope.toByteArray()));
    }

    @Test
    public void bigDemo() {
        byte[] fixedBytes = Hex.decode("25C0513DDF98");

        OpenAccessCredentialFormat.CredentialEnvelope envelope =
            OpenAccessCredentialFormat.CredentialEnvelope.newBuilder()
                .addPacsRecord(
                        OpenAccessCredentialFormat.PacsRecord.newBuilder()
                                .setWiegandData(ByteString.copyFrom(fixedBytes))
                                .build())
                    .setToken(
                        OpenAccessCredentialFormat.TokenInfo.newBuilder()
                                .setUid(ByteString.copyFrom(new byte[] {
                                        0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x02, 0x04
                                })))
                .build();

        System.out.println(envelope.toString());
        System.out.println("************" + Hex.encode(fixedBytes));
        System.out.println(Hex.encode(envelope.toByteArray()));
    }

}
