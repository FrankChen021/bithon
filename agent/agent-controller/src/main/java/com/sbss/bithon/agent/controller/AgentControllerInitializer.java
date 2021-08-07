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

import com.sbss.bithon.agent.bootstrap.loader.AgentClassLoader;
import com.sbss.bithon.agent.controller.cmd.IAgentCommand;
import com.sbss.bithon.agent.controller.setting.AgentSettingManager;
import com.sbss.bithon.agent.core.config.AgentConfigManager;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.PluginClassLoaderManager;
import com.sbss.bithon.agent.core.starter.IAgentInitializer;
import com.sbss.bithon.agent.core.utils.StringUtils;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/1 5:55 下午
 */
public class AgentControllerInitializer implements IAgentInitializer {
    private static final Logger log = LoggerFactory.getLogger(AgentControllerInitializer.class);

    @Override
    public void initialize(AgentContext context) throws Exception {
        log.info("Initializing agent controller");

        AgentControllerConfig ctrlConfig = AgentConfigManager.getInstance().getConfig(AgentControllerConfig.class);
        if (ctrlConfig == null || StringUtils.isEmpty(ctrlConfig.getClient())) {
            log.warn("Agent Controller has not configured.");
            return;
        }

        //
        // create controller
        //
        IAgentController controller = null;
        try {
            IAgentControllerFactory factory = (IAgentControllerFactory) Class.forName(ctrlConfig.getClient())
                                                                             .newInstance();
            controller = factory.createController(ctrlConfig);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("Can't create instanceof fetcher {}", ctrlConfig.getClient());
            throw e;
        }

        loadAgentCommands(controller, AgentClassLoader.getClassLoader());
        loadAgentCommands(controller, PluginClassLoaderManager.getDefaultLoader());

        //
        // start fetcher
        //
        AgentSettingManager.createInstance(context.getAppInstance().getRawAppName(),
                                           context.getAppInstance().getEnv(),
                                           controller);
    }

    private void loadAgentCommands(IAgentController controller, ClassLoader classLoader) {
        for (IAgentCommand agentCommand : ServiceLoader.load(IAgentCommand.class,
                                                             classLoader)) {

            log.info("Binding agent commands provided by {}", agentCommand.getClass().getSimpleName());

            controller.attachCommands(agentCommand);
        }
    }
}
