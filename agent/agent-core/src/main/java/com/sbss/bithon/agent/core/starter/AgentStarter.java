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

package com.sbss.bithon.agent.core.starter;

import com.sbss.bithon.agent.bootstrap.aop.BootstrapHelper;
import com.sbss.bithon.agent.bootstrap.loader.AgentClassLoader;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.loader.PluginClassLoaderManager;
import com.sbss.bithon.agent.core.plugin.loader.PluginInstaller;
import shaded.org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ServiceLoader;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class AgentStarter {

    public void start(String agentPath, Instrumentation inst) throws Exception {
        BootstrapHelper.setPluginClassLoader(PluginClassLoaderManager.createDefault(agentPath));

        initAgentLogger(agentPath);

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        ensureApplicationTempDirectory(agentContext);

        PluginInstaller.install(agentContext, inst);

        // initialize other agent libs
        for (IAgentInitializer initializer : ServiceLoader.load(IAgentInitializer.class,
                                                                AgentClassLoader.getClassLoader())) {
            initializer.initialize(agentContext);
        }
    }

    private void initAgentLogger(String agentPath) {
        String logConfigName = "log4j.configuration";
        String logConfigFile = agentPath + separator + AgentContext.CONF_DIR + separator + "log4j.xml";
        String oldLogConfig = System.getProperty(logConfigName);

        // replace original property
        System.setProperty(logConfigName, logConfigFile);

        // log config
        DOMConfigurator.configure(logConfigFile);

        // restore original property
        if (oldLogConfig != null) {
            System.setProperty(logConfigName, oldLogConfig);
        } else {
            System.clearProperty(logConfigName);
        }
    }

    private void ensureApplicationTempDirectory(AgentContext context) {
        File tmpDir = new File(context.getAgentDirectory() + separator + AgentContext.TMP_DIR + separator +
                               context.getConfig().getBootstrap().getAppName());

        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }
}
