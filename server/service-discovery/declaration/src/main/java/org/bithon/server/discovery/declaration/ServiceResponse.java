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
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Row-based Response.
 *
 * @author Frank Chen
 * @date 2022/8/7 21:29
 */
public class ServiceResponse<T> {
    public final static ServiceResponse EMPTY = new ServiceResponse(Collections.emptyList());

    /**
     * column names
     */
    @Getter
    private final List<T> rows;

    @Getter
    private final Map<String, String> error;

    public ServiceResponse(List<T> rows) {
        this.rows = rows;
        this.error = null;
    }

    public ServiceResponse(Map<String, String> error) {
        this.rows = null;
        this.error = error;
    }

    /**
     * A ctor for JSON deserialization by Jackson
     */
    @JsonCreator
    public ServiceResponse(@JsonProperty("rows") List<T> rows,
                           @JsonProperty("error") Map<String, String> error) {
        this.rows = rows;
        this.error = error;
    }

    public static <T> ServiceResponse<T> success(List<T> rows) {
        return new ServiceResponse<>(rows);
    }

    public static <T> ServiceResponse error(Map<String, String> error) {
        return new ServiceResponse(error);
    }
}
