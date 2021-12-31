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

import java.util.HashMap;

/**
 * @author Frank Chen
 * @date 31/12/21 11:25 PM
 */
@ConfigurationProperties(prefix = "agent.plugin.spring.webflux.gateway")
public class GatewayFilterConfig extends HashMap<String, String> {
}
