/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.setting;

import com.sbss.bithon.agent.rpc.brpc.BrpcMessageHeader;
import com.sbss.bithon.agent.rpc.brpc.ISettingFetcher;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/30 3:34 下午
 */
public class BrpcSettingFetcher implements ISettingFetcher {

    private final AgentSettingService settingService;

    public BrpcSettingFetcher(AgentSettingService settingService) {
        this.settingService = settingService;
    }

    @Override
    public Map<String, String> fetch(BrpcMessageHeader header, long lastModifiedSince) {
        return settingService.getSettings(header.getAppName(), header.getEnv(), lastModifiedSince);
    }
}
