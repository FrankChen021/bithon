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

import org.bithon.agent.bootstrap.loader.AgentClassLoader;
import org.bithon.agent.bootstrap.loader.PluginClassLoaderManager;
import org.bithon.agent.controller.cmd.IAgentCommand;
import org.bithon.agent.controller.setting.AgentSettingManager;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.starter.IAgentLifeCycle;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ServiceLoader;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/1 5:55 下午
 */
public class AgentControllerLifeCycle implements IAgentLifeCycle {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AgentControllerLifeCycle.class);

    @Override
    public void start(AgentContext context) throws Exception {
        LOG.info("Initializing agent controller");

        AgentControllerConfig ctrlConfig = ConfigurationManager.getInstance().getConfig(AgentControllerConfig.class);
        if (ctrlConfig == null || StringUtils.isEmpty(ctrlConfig.getClient())) {
            LOG.warn("Agent Controller has not configured.");
            return;
        }

        //
        // create controller
        //
        IAgentController controller;
        try {
            IAgentControllerFactory factory = (IAgentControllerFactory) Class.forName(ctrlConfig.getClient())
                                                                             .getDeclaredConstructor().newInstance();
            controller = factory.createController(ctrlConfig);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Can't create instanceof fetcher {}", ctrlConfig.getClient());
            throw e;
        }

        loadAgentCommands(controller, AgentClassLoader.getClassLoader());
        loadAgentCommands(controller, PluginClassLoaderManager.getDefaultLoader());

        //
        // start fetcher
        //
        AgentSettingManager.createInstance(context.getAppInstance().getAppName(),
                                           context.getAppInstance().getEnv(),
                                           controller);
    }

    @Override
    public void stop() {
        
    }

    private void loadAgentCommands(IAgentController controller, ClassLoader classLoader) {
        for (IAgentCommand agentCommand : ServiceLoader.load(IAgentCommand.class,
                                                             classLoader)) {

            LOG.info("Binding agent commands provided by {}", agentCommand.getClass().getSimpleName());

            controller.attachCommands(agentCommand);
        }
    }
}
