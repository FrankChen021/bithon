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

package org.bithon.server.discovery.declaration.cmd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * @author Frank Chen
 * @date 2022/8/7 21:20
 */
public class CommandArgs<T> {
    /**
     * unique client app id
     */
    @NotNull
    @Getter
    private final String appId;

    @Getter
    private final T args;

    @JsonCreator
    public CommandArgs(@JsonProperty("appId") String appId,
                       @JsonProperty("args") T args) {
        this.appId = appId;
        this.args = args;
    }
}
