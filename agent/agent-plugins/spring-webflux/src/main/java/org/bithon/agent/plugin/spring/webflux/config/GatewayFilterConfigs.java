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

package org.bithon.agent.plugin.spring.webflux.config;

import org.bithon.agent.core.config.ConfigurationProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 31/12/21 11:25 PM
 */
@ConfigurationProperties(prefix = "agent.plugin.spring.webflux.gateway", dynamic = false)
public class GatewayFilterConfigs extends HashMap<String, GatewayFilterConfigs.Filter> {
    public static class Filter {
        private String mode;

        /**
         * defines which attributes on the {@link import org.springframework.web.server.ServerWebExchange} object should be recorded to the span log
         */
        private Map<String, String> attributes = Collections.emptyMap();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        /**
         * key - the prop name in the ServerExchange.attributes
         * val - the value that is written into span's tags
         */
        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
    }
}
