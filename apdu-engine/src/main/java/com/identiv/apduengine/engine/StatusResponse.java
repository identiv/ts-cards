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
package com.identiv.apduengine.engine;

import com.google.common.base.Optional;

import java.io.Serializable;

/**
 */
public class StatusResponse implements Serializable {

    ApduStatus status;
    Optional<ApduResponse> response;

    public StatusResponse() {
    }

    public StatusResponse(ApduStatus status) {
        this.status = status;
        this.response = Optional.absent();
    }

    public StatusResponse(ApduStatus status, ApduResponse response) {
        this.status = status;
        setResponse(response);
    }

    public ApduStatus getStatus() {
        return status;
    }

    public void setStatus(ApduStatus status) {
        this.status = status;
    }

    public Optional<ApduResponse> getResponse() {
        return response;
    }

    public void setResponse(ApduResponse response) {
        if (response == null) {
            this.response = Optional.absent();
        } else {
            this.response = Optional.of(response);

        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("StatusResponse{");
        sb.append("status=").append(status);
        sb.append(", response=").append(response.orNull());
        sb.append('}');
        return sb.toString();
    }
}
