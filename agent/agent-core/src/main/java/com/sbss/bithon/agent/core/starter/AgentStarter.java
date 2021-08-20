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

import com.sbss.bithon.agent.bootstrap.loader.AgentClassLoader;
import com.sbss.bithon.agent.core.aop.InstrumentationHelper;
import com.sbss.bithon.agent.core.config.AppConfiguration;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.PluginInterceptorInstaller;
import shaded.org.apache.log4j.xml.DOMConfigurator;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ServiceLoader;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class AgentStarter {

    public void start(String agentPath, Instrumentation inst) throws Exception {
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

    private void initAgentLogger(String agentPath) {
        String logConfigName = "log4j.configuration";
        String logConfigFile = agentPath + separator + AgentContext.CONF_DIR + separator + "log4j.xml";
        String oldLogConfig = System.getProperty(logConfigName);

        // replace original property
        System.setProperty(logConfigName, logConfigFile);

        // log config
        ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(AgentClassLoader.getClassLoader());
            DOMConfigurator.configure(logConfigFile);
        } finally {
            Thread.currentThread().setContextClassLoader(ctxLoader);
        }

        // restore original property
        if (oldLogConfig != null) {
            System.setProperty(logConfigName, oldLogConfig);
        } else {
            System.clearProperty(logConfigName);
        }
    }

    private void ensureApplicationTempDirectory(AgentContext context) {
        AppConfiguration appConfiguration = context.getAgentConfiguration().getConfig(AppConfiguration.class);

        File tmpDir = new File(context.getAgentDirectory() + separator + AgentContext.TMP_DIR + separator +
                               appConfiguration.getName());

        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }
}
