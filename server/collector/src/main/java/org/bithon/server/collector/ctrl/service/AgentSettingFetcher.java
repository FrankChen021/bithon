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

package org.bithon.server.collector.ctrl.service;

import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.setting.ISettingFetcher;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.setting.ISettingReader;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/30 3:34 下午
 */
public class AgentSettingFetcher implements ISettingFetcher {

    private final ISettingReader reader;

    public AgentSettingFetcher(ISettingReader reader) {
        this.reader = reader;
    }

    @Override
    public Map<String, String> fetch(BrpcMessageHeader header, long lastModifiedSince) {
        // Always fetch all configuration by setting 'since' parameter to 0
        return getConfiguration(header.getAppName(), header.getEnv(), 0);
    }

    private Map<String, String> getConfiguration(String appName, String env, long since) {
        if (StringUtils.hasText(env)) {
            appName += "-" + env;
        }
        return reader.getSettings(appName, since);
    }
}
