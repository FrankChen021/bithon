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

package org.bithon.server.agent.controller.config;

import lombok.Data;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Frank Chen
 * @date 24/1/24 9:41 pm
 */
@Data
@Configuration
@ConfigurationProperties("bithon.agent-controller")
public class AgentControllerConfig {
    private boolean enabled;
    private int port;

    @Data
    public static class ChannelConfig {
        /**
         * Watermark configuration for underlying communication channel.
         */
        private HumanReadableNumber lowWaterMark;
        private HumanReadableNumber highWaterMark;
    }

    private ChannelConfig channel;

    private PermissionConfig permission;
}
