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

import com.identiv.apduengine.engine.util.Hex;

import java.io.Serializable;

/** 
 * Simple name/value pair holder with convenience methods for converting
 * to/from byte[] to Hex strings.
 */
public class ApduSessionParameter implements Serializable {

    String name;
    String value;

    public ApduSessionParameter() {
    }

    public ApduSessionParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public ApduSessionParameter(String name, byte[] value) {
        this.name = name;
        setValue(value);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApduSessionParameter that = (ApduSessionParameter) o;

        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;
        return value != null ? value.equals(that.value) : that.value == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte[] getValueAsBytes() {
        return Hex.decode(value);
    }

    public void setValue(byte[] value) {
        setValue(Hex.encode(value));
    }

    public void setValue(byte[] value, int index, int length) {
        byte[] holder = new byte[length];
        System.arraycopy(value, index,
                holder, 0, length);
        setValue(holder);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApduSessionParameter {");
        sb.append("name='").append(name).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
