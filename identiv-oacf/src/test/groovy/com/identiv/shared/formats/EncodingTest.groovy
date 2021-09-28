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
import org.apache.commons.codec.binary.Hex;
import spock.lang.Specification
import org.apache.commons.codec.DecoderException;


import static com.identiv.shared.formats.OpenAccessCredentialFormat.CredentialEnvelope;

/**
 */
class EncodingTest extends Specification {

    public static byte[] decodeHex(String input) {
        try {
            return Hex.decodeHex(input.toCharArray());
        } catch (DecoderException e) {
            throw new IllegalStateException("Hex Decoder exception", e);
        }
    }

    void "simple format examples"(String payload, String result) {
        when:
            // encode
            byte[] payloadBytes = decodeHex(payload)
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
            Hex.encodeHexString(envelopeBytes) == result
            Hex.encodeHexString(decoded) == payload

        where:
            payload         || result
            "250000000150"  || "0a082206250000000150"
            "230028001a60"  || "0a082206230028001a60"
            "1a00110011"    || "0a0722051a00110011"
            "2502479d9970"  || "0a0822062502479d9970"

    }

    void "complex format examples"(String payload, String uid, String result) {
        when:
            byte[] payloadBytes = decodeHex(payload)
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
                                decodeHex(uid)))
                            .build())
                    .build();
            byte[] envelopeBytes = envelope.toByteArray()
        then:
            Hex.encodeHexString(envelopeBytes) == result
        where:
            payload          | uid                || result
            "250000000150"   | "0001020304050607" ||
                                 "0a0822062500000001501a0a3a080001020304050607"
            "230028001a60"   | "0102030405060708" ||
                                 "0a082206230028001a601a0a3a080102030405060708"
            "1a00110011"     | "0203040506070809" ||
                                 "0a0722051a001100111a0a3a080203040506070809"
    }

}
