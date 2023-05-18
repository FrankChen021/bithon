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

package org.bithon.agent.controller;

import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.observability.dispatcher.config.DispatcherClient;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 2:48 下午
 */
@ConfigurationProperties(prefix = "controller", dynamic = false)
public class AgentControllerConfig {
    private DispatcherClient client;
    private String servers;

    public DispatcherClient getClient() {
        return client;
    }

    public void setClient(DispatcherClient client) {
        this.client = client;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }
}
