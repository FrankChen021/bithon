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
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 24/3/22 12:25 PM
 */
@ConfigurationProperties(prefix = "agent.plugin.spring.webflux.response")
public class ResponseConfigs {
    private Map<String, String> headers = Collections.emptyMap();

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
