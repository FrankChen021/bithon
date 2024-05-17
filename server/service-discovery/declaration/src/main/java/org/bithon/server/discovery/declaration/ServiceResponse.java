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

package org.bithon.server.discovery.declaration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Row-based Response.
 *
 * @author Frank Chen
 * @date 2022/8/7 21:29
 */
public class ServiceResponse<T> {
    public static final ServiceResponse EMPTY = new ServiceResponse<>(Collections.emptyList());

    @Data
    @Builder
    public static class Error {
        private String uri;
        private String exception;
        private String message;

        /**
         * Add jackson annotation so that a client can deserialize the response manually.
         */
        @JsonCreator
        public Error(@JsonProperty("uri") String uri,
                     @JsonProperty("exception") String exception,
                     @JsonProperty("message") String message) {
            this.uri = uri;
            this.exception = exception;
            this.message = message;
        }
    }

    /**
     * column names
     */
    @Getter
    private final List<T> rows;

    /**
     * In some cases, we want to return the error object while at the interface level(for example at HTTP side), the response is successful
     */
    @Getter
    private final Error error;

    public ServiceResponse(List<T> rows) {
        this.rows = rows;
        this.error = null;
    }

    public ServiceResponse(Error error) {
        this.rows = null;
        this.error = error;
    }

    /**
     * A ctor for JSON deserialization by Jackson
     */
    @JsonCreator
    public ServiceResponse(@JsonProperty("rows") List<T> rows,
                           @JsonProperty("error") Error error) {
        this.rows = rows;
        this.error = error;
    }

    public static <T> ServiceResponse<T> success(List<T> rows) {
        return new ServiceResponse<>(rows);
    }

    public static <T> ServiceResponse error(Error error) {
        return new ServiceResponse(error);
    }
}
