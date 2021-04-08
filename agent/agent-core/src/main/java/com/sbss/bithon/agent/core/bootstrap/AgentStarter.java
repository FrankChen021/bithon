package com.sbss.bithon.agent.core.bootstrap;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.loader.PluginDependencyManager;
import com.sbss.bithon.agent.core.plugin.loader.PluginInstaller;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import shaded.org.apache.logging.log4j.core.config.ConfigurationSource;
import shaded.org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.instrument.Instrumentation;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class AgentStarter {

    public void start(String agentPath, Instrumentation inst) throws Exception {
        PluginDependencyManager.initialize();

        initAgentLogger(agentPath);

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        ensureApplicationTempDirectory(agentContext);

        // init setting
        AgentSettingManager.createInstance(agentContext.getAppInstance(),
                                           agentContext.getConfig().getFetcher());

        PluginInstaller.install(agentContext, inst);
    }

    private void initAgentLogger(String agentPath) throws IOException {


        String logConfigName = "log4j.configurationFile";
        String logConfigFile = agentPath + separator + AgentContext.CONF_DIR + separator + "log4j.xml";
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
                               context.getConfig().getBootstrap().getAppName());

        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }
}
