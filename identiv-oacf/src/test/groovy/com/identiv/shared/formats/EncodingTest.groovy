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
package com.identiv.shared.formats

import com.google.protobuf.ByteString
import com.idondemand.client.utils.Hex
import spock.lang.Specification


import static com.identiv.shared.formats.OpenAccessCredentialFormat.CredentialEnvelope;

/**
 */
class EncodingTest extends Specification {

    void "simple format examples"(String payload, String result) {
        when:
            // encode
            byte[] payloadBytes = Hex.decode(payload)
            OpenAccessCredentialFormat.CredentialEnvelope envelope =
                OpenAccessCredentialFormat.CredentialEnvelope.newBuilder()
                    .setDefaultPacsRecord(
                        OpenAccessCredentialFormat.PacsRecord.newBuilder()
                                .setWiegandData(ByteString.copyFrom(payloadBytes))
                                .build())
                    .build();
            byte[] envelopeBytes = envelope.toByteArray()

            // and decode
            envelope = CredentialEnvelope.parseFrom(envelopeBytes);
            byte[] decoded = envelope.getDefaultPacsRecord()
                    .getWiegandData().toByteArray()

        then:
            Hex.encode(envelopeBytes) == result
            Hex.encode(decoded) == payload

        where:
            payload         || result
            "250000000150"  || "0A082206250000000150"
            "230028001A60"  || "0A082206230028001A60"
            "1A00110011"    || "0A0722051A00110011"
            "2502479D9970"  || "0A0822062502479D9970"

    }

    void "complex format examples"(String payload, String uid, String result) {
        when:
            byte[] payloadBytes = Hex.decode(payload)
            OpenAccessCredentialFormat.CredentialEnvelope envelope =
                OpenAccessCredentialFormat.CredentialEnvelope.newBuilder()
                    .setDefaultPacsRecord(
                        OpenAccessCredentialFormat.PacsRecord.newBuilder()
                            .setWiegandData(ByteString.copyFrom(
                                payloadBytes))
                            .build())
                    .setToken(
                        OpenAccessCredentialFormat.TokenInfo.newBuilder()
                            .setUid(ByteString.copyFrom(
                                Hex.decode(uid)))
                            .build())
                    .build();
            byte[] envelopeBytes = envelope.toByteArray()
        then:
            Hex.encode(envelopeBytes) == result
        where:
            payload          | uid                || result
            "250000000150"   | "0001020304050607" ||
                                 "0A0822062500000001501A0A3A080001020304050607"
            "230028001A60"   | "0102030405060708" ||
                                 "0A082206230028001A601A0A3A080102030405060708"
            "1A00110011"     | "0203040506070809" ||
                                 "0A0722051A001100111A0A3A080203040506070809"
    }

}
