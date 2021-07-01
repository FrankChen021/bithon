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

package com.sbss.bithon.agent.controller;

import com.sbss.bithon.agent.controller.setting.AgentSettingManager;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.starter.IAgentInitializer;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/1 5:55 下午
 */
public class Initializer implements IAgentInitializer {
    private static final Logger log = LoggerFactory.getLogger(Initializer.class);

    @Override
    public void initialize(AgentContext context) throws Exception {
        log.info("Initializing agent controller");

        // init setting
        AgentSettingManager.createInstance(context.getAppInstance(),
                                           context.getConfig().getFetcher());

    }
}
