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

package com.sbss.bithon.agent.dispatcher.netty;

import com.sbss.bithon.agent.core.config.FetcherConfig;
import com.sbss.bithon.agent.core.setting.IAgentSettingFetcher;
import com.sbss.bithon.agent.core.setting.IAgentSettingFetcherFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 4:40 下午
 */
public class SettingFetcherFactory implements IAgentSettingFetcherFactory {
    @Override
    public IAgentSettingFetcher createFetcher(FetcherConfig config) {
        return new SettingFetcher(config);
    }
}
