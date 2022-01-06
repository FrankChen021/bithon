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
import java.util.List;

/**
 * @author Frank Chen
 * @date 31/12/21 11:25 PM
 */
@ConfigurationProperties(prefix = "agent.plugin.spring.webflux.gateway")
public class GatewayFilterConfigs extends HashMap<String, GatewayFilterConfigs.Filter> {
    public static class Filter {
        private String mode;
        private List<String> attributes = Collections.emptyList();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<String> attributes) {
            this.attributes = attributes;
        }
    }
}
