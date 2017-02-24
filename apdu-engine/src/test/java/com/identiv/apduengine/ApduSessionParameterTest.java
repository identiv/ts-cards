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

import org.junit.Assert;
import org.junit.Test;

/**
 */
public class ApduSessionParameterTest {

    @Test
    public void setValueBytes() {
        ApduSessionParameter parameter = new ApduSessionParameter();
        parameter.setName("Test Parameter");

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};

        parameter.setValue(input);

        String value = parameter.getValue();

        Assert.assertEquals("010203040506", value);

    }

    @Test
    public void setValueByArraySlice() {
        ApduSessionParameter parameter = new ApduSessionParameter();
        parameter.setName("Test Parameter");

        byte[] input = new byte[]{1, 2, 3, 4, 5, 6};

        parameter.setValue(input, 2, 3);

        String value = parameter.getValue();

        Assert.assertEquals("030405", value);
    }

    @Test
    public void getValueAsBytes() {
        ApduSessionParameter parameter = new ApduSessionParameter();
        parameter.setName("Test Parameter");

        parameter.setValue("0102030405");

        byte[] value = parameter.getValueAsBytes();

        Assert.assertArrayEquals(
                new byte[]{1, 2, 3, 4, 5},
                value);
    }
}
