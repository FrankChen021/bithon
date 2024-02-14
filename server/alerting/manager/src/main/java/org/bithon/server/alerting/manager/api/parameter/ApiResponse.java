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

package org.bithon.server.alerting.manager.api.parameter;

import lombok.Data;

/**
 * @author Frank Chen
 * @date 12/11/21 2:49 pm
 */
@Data
public class ApiResponse<T> {
    private T data;
    private int code = 200;
    private String message;

    public ApiResponse(String message) {
        this.message = message;
        this.code = 500;
    }

    public ApiResponse() {
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(message);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse(data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>((T) null);
    }
}
