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

package org.bithon.agent.core.starter;

import org.bithon.agent.AgentBuildVersion;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.loader.AgentClassLoader;
import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.agent.core.aop.installer.InterceptorInstaller;
import org.bithon.agent.core.config.AppConfiguration;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.context.AppInstance;
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.plugin.PluginResolver;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author frankchen
 */
public class AgentStarter {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AgentStarter.class);

    /**
     * The banner is generated at https://manytools.org/hacker-tools/ascii-banner/ with font = 3D-ASCII
     */
    private static void showBanner() {
        LOG.info("\n ________  ___  _________  ___  ___  ________  ________      \n"
                 + "|\\   __  \\|\\  \\|\\___   ___\\\\  \\|\\  \\|\\   __  \\|\\   ___  \\    \n"
                 + "\\ \\  \\|\\ /\\ \\  \\|___ \\  \\_\\ \\  \\\\\\  \\ \\  \\|\\  \\ \\  \\\\ \\  \\   \n"
                 + " \\ \\   __  \\ \\  \\   \\ \\  \\ \\ \\   __  \\ \\  \\\\\\  \\ \\  \\\\ \\  \\  \n"
                 + "  \\ \\  \\|\\  \\ \\  \\   \\ \\  \\ \\ \\  \\ \\  \\ \\  \\\\\\  \\ \\  \\\\ \\  \\ \n"
                 + "   \\ \\_______\\ \\__\\   \\ \\__\\ \\ \\__\\ \\__\\ \\_______\\ \\__\\\\ \\__\\\n"
                 + "    \\|_______|\\|__|    \\|__|  \\|__|\\|__|\\|_______|\\|__| \\|__|\n"
                 + "Version: {}, {}, Build time:{}\n",
                 AgentBuildVersion.VERSION,
                 AgentBuildVersion.SCM_REVISION,
                 AgentBuildVersion.TIMESTAMP);
    }

    public void start(String agentPath, Instrumentation inst) throws Exception {
        showBanner();

        InstrumentationHelper.setInstance(inst);

        //
        // show loaded libs
        //
        AgentClassLoader.getClassLoader()
                        .getJars()
                        .stream()
                        .map(jar -> new File(jar.getName()).getName())
                        .sorted()
                        .forEach(name -> LOG.info("Found lib {}", name));
        AgentContext agentContext = AgentContext.createInstance(agentPath);

        ConfigurationManager.create(agentPath);
        agentContext.setAppInstance(new AppInstance(ConfigurationManager.getInstance().getConfig(AppConfiguration.class)));

        // install interceptors for plugins
        new InterceptorInstaller(new PluginResolver(agentContext).resolveInterceptorDescriptors())
            .installOn(createAgentBuilder(inst), inst);

        // Initialize other agent libs
        final List<IAgentService> services = new ArrayList<>();
        for (IAgentService lifecycle : ServiceLoader.load(IAgentService.class,
                                                          AgentClassLoader.getClassLoader())) {
            services.add(lifecycle);
        }
        // Sort the services in their priority
        services.sort((o1, o2) -> o2.getOrder() - o1.getOrder());

        //
        for (IAgentService service : services) {
            service.start(agentContext);
        }

        // register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // stop each life cycle object
            // the last started life cycle object will be stopped in first
            for (int i = services.size() - 1; i >= 0; i--) {
                try {
                    services.get(i).stop();
                } catch (Exception ignored) {
                }
            }

            // destroy the dispatchers at last
            for (Dispatcher dispatcher : Dispatchers.getAllDispatcher()) {
                dispatcher.shutdown();
            }
        }, "agentShutdown"));
    }

    private static AgentBuilder createAgentBuilder(Instrumentation inst) {
        AgentBuilder builder = new AgentBuilder.Default();

        builder = builder.assureReadEdgeFromAndTo(inst, IBithonObject.class);

        return builder;
    }
}
