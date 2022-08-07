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

package org.bithon.server.collector.cmd.api;

import lombok.Getter;

/**
 * @author Frank Chen
 * @date 2022/8/7 21:29
 */
public class CommandResponse<T> {
    @Getter
    private final T data;

    @Getter
    private final String error;

    public CommandResponse(T data) {
        this.data = data;
        this.error = null;
    }

    public CommandResponse(String error) {
        this.data = null;
        this.error = error;
    }

    public static <T> CommandResponse<T> success(T data) {
        return new CommandResponse<>(data);
    }

    public static <T> CommandResponse<T> error(String error) {
        return new CommandResponse<>(error);
    }

    public static <T> CommandResponse<T> exception(Throwable throwable) {
        return new CommandResponse<>(throwable.getMessage());
    }
}
