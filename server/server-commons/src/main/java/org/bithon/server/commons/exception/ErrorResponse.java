/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.commons.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author Frank Chen
 * @date 21/4/23 1:40 pm
 */
@Data
@Builder
public class ErrorResponse {
    private String path;
    private String message;
    private String exception;

    /**
     * Add jackson annotation so that clients can deserialize the response manually.
     *
     * NOTE: The order or parameters MUST be the same as the order of declared fields.
     */
    @JsonCreator
    public ErrorResponse(@JsonProperty("path") String path,
                         @JsonProperty("message") String message,
                         @JsonProperty("exception") String exception) {
        this.path = path;
        this.message = message;
        this.exception = exception;
    }
}
