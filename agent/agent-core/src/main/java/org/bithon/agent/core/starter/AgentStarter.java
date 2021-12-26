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
import org.bithon.agent.bootstrap.loader.AgentClassLoader;
import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.plugin.PluginInterceptorInstaller;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ServiceLoader;

/**
 * @author frankchen
 */
public class AgentStarter {
    private static ILogAdaptor log = LoggerFactory.getLogger(AgentStarter.class);

    /**
     * The banner is generated at https://manytools.org/hacker-tools/ascii-banner/ with font = 3D-ASCII
     */
    private static void showBanner() {
        log.info("\n ________  ___  _________  ___  ___  ________  ________      \n"
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
                        .forEach(name -> log.info("Found lib {}", name));

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        PluginInterceptorInstaller.install(agentContext, inst);

        // initialize other agent libs
        for (IAgentInitializer initializer : ServiceLoader.load(IAgentInitializer.class,
                                                                AgentClassLoader.getClassLoader())) {
            initializer.initialize(agentContext);
        }
    }
}
