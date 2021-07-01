/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.controller.cmd;

public class AgentCommandResponse<T> {
    private final Integer code;
    private final String message;
    private final T data;

    private AgentCommandResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public static AgentCommandResponse<?> fail(int code, String message) {
        return new AgentCommandResponse<>(code, null, message);
    }

    public static AgentCommandResponse<?> fail(int code, String message, Object... args) {
        return new AgentCommandResponse<>(code, null, String.format(message, args));
    }

    public static <T> AgentCommandResponse<T> success(T data) {
        return new AgentCommandResponse<>(0, data, "success");
    }

    public static AgentCommandResponse<?> SUCCESS = new AgentCommandResponse<>(0, null, "success");

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
