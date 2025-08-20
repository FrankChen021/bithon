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

import org.bithon.agent.config.RpcClientConfig;
import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.annotation.PropertyDescriptor;
import org.bithon.agent.configuration.validation.GreaterThan;
import org.bithon.agent.configuration.validation.Validated;


/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 2:48 下午
 */
@ConfigurationProperties(path = "controller", dynamic = false)
public class AgentControllerConfig {
    @Validated
    private RpcClientConfig client;

    @PropertyDescriptor(description = "The list of the agent controller endpoint separated in comma, e.g. host1:port1,host2:port2...")
    private String servers;

    @PropertyDescriptor(
        description = "The interval in seconds that how long the agent sync its configuration with the remote controller",
        required = false
    )
    @GreaterThan(value = 0)
    private int refreshInterval = 60;

    public RpcClientConfig getClient() {
        return client;
    }

    public void setClient(RpcClientConfig client) {
        this.client = client;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }
}
