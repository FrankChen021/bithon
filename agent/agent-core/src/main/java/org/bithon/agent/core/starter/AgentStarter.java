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
import shaded.org.apache.logging.log4j.core.config.ConfigurationSource;
import shaded.org.apache.logging.log4j.core.config.Configurator;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Locale;
import java.util.ServiceLoader;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class AgentStarter {

    public void start(String agentPath, Instrumentation inst) throws Exception {
        System.out.printf(Locale.ENGLISH,
                          "Version: %s, %s, Build time:%s%n%n",
                          AgentBuildVersion.VERSION,
                          AgentBuildVersion.SCM_REVISION,
                          AgentBuildVersion.TIMESTAMP);

        InstrumentationHelper.setInstance(inst);

        initAgentLogger(agentPath);

        //
        // show loaded libs
        //
        AgentClassLoader.getClassLoader()
                        .getJars()
                        .stream()
                        .map(jar -> new File(jar.getName()).getName())
                        .sorted()
                        .forEach(name -> LoggerFactory.getLogger("AgentClassLoader").info("Found lib {}", name));

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        ensureApplicationTempDirectory(agentContext);

        PluginInterceptorInstaller.install(agentContext, inst);

        // initialize other agent libs
        for (IAgentInitializer initializer : ServiceLoader.load(IAgentInitializer.class,
                                                                AgentClassLoader.getClassLoader())) {
            initializer.initialize(agentContext);
        }
    }

    private void initAgentLogger(String agentPath) throws IOException {
        String logConfigName = "log4j.configurationFile";
        String logConfigFile = agentPath + separator + AgentContext.CONF_DIR + separator + "log4j2.xml";
        String oldLogConfig = System.getProperty(logConfigName);

        // replace original property
        System.setProperty(logConfigName, logConfigFile);

        // log config
        Configurator.initialize(null,
                                new ConfigurationSource(new FileInputStream(logConfigFile),
                                                        new File(logConfigFile).toURI().toURL()));

        // restore original property
        if (oldLogConfig != null) {
            System.setProperty(logConfigName, oldLogConfig);
        } else {
            System.clearProperty(logConfigName);
        }
    }

    private void ensureApplicationTempDirectory(AgentContext context) {
        File tmpDir = new File(context.getAgentDirectory() + separator + AgentContext.TMP_DIR + separator +
                               context.getAppInstance().getQualifiedAppName());

        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }
}
