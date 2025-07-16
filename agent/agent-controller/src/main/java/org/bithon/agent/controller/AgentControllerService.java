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

import org.bithon.agent.config.AppConfig;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.controller.cmd.IAgentCommand;
import org.bithon.agent.controller.cmd.InstrumentationCommand;
import org.bithon.agent.controller.cmd.JvmCommand;
import org.bithon.agent.controller.cmd.LoggingCommand;
import org.bithon.agent.controller.cmd.ProfilingCommand;
import org.bithon.agent.controller.config.AgentSettingFetchTask;
import org.bithon.agent.controller.config.ConfigurationCommandImpl;
import org.bithon.agent.instrumentation.loader.AgentClassLoader;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.agent.starter.IAgentService;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.time.Duration;
import java.util.ServiceLoader;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/1 5:55 下午
 */
public class AgentControllerService implements IAgentService {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AgentControllerService.class);

    private static IAgentController controller;
    private AgentSettingFetchTask fetchTask;

    public static IAgentController getControllerInstance() {
        return controller;
    }

    @Override
    public void start() throws Exception {
        LOG.info("Initializing agent controller");

        AgentControllerConfig ctrlConfig = ConfigurationManager.getInstance().getConfig(AgentControllerConfig.class);
        if (ctrlConfig == null || ctrlConfig.getClient() == null || StringUtils.isEmpty(ctrlConfig.getClient().getFactory())) {
            LOG.warn("Agent Controller has not configured.");
            return;
        }

        //
        // create controller
        //
        try {
            IAgentControllerFactory factory = (IAgentControllerFactory) Class.forName(ctrlConfig.getClient().getFactory())
                                                                             .getDeclaredConstructor().newInstance();
            controller = factory.createController(ctrlConfig);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            LOG.error("Can't create instanceof fetcher {}", ctrlConfig.getClient());
            throw e;
        }

        attachCommand(controller, new JvmCommand());
        attachCommand(controller, new InstrumentationCommand());
        attachCommand(controller, new ConfigurationCommandImpl());
        attachCommand(controller, new LoggingCommand());
        attachCommand(controller, new ProfilingCommand());
        loadAgentCommands(controller, AgentClassLoader.getClassLoader());
        loadAgentCommands(controller, PluginClassLoader.getClassLoader());


        //
        // Start fetcher
        //
        AppConfig appConfig = ConfigurationManager.getInstance().getConfig(AppConfig.class);
        fetchTask = new AgentSettingFetchTask(appConfig.getName(),
                                              appConfig.getEnv(),
                                              controller,
                                              Duration.ofSeconds(ctrlConfig.getRefreshInterval()));
        fetchTask.start();
    }

    @Override
    public void stop() {
        // Stop the task first because the task relies on the controller internally
        this.fetchTask.stop();

        try {
            controller.close();
        } catch (Exception ignored) {
        }
    }

    private void loadAgentCommands(IAgentController controller, ClassLoader classLoader) {
        for (IAgentCommand agentCommand : ServiceLoader.load(IAgentCommand.class,
                                                             classLoader)) {
            attachCommand(controller, agentCommand);
        }
    }

    private void attachCommand(IAgentController controller, IAgentCommand agentCommand) {
        LOG.info("Binding agent commands provided by {}", agentCommand.getClass().getSimpleName());

        controller.attachCommands(agentCommand);
    }
}
