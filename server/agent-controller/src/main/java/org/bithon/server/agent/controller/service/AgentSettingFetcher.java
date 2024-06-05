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

package org.bithon.server.agent.controller.service;

import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.setting.ISettingFetcher;

import java.util.Map;

/**
 * The BRPC implementation to get agent settings for agents.
 *
 * @author frank.chen021@outlook.com
 * @date 2021/6/30 3:34 下午
 */
@Slf4j
public class AgentSettingFetcher implements ISettingFetcher {

    private final AgentSettingLoader loader;

    public AgentSettingFetcher(AgentSettingLoader loader) {
        this.loader = loader;
    }

    @Override
    public Map<String, String> fetch(BrpcMessageHeader header, long lastModifiedSince) {
        return this.loader.get(header.getAppName(), header.getEnv());
    }
}
