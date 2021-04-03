package com.sbss.bithon.agent.core.bootstrap;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.loader.PluginDependencyManager;
import com.sbss.bithon.agent.core.plugin.loader.PluginInstaller;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import shaded.org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.lang.instrument.Instrumentation;

import static java.io.File.separator;

public class AgentStarter {
    private static final String CONF_LOG_FILE = "log4j.xml";

    public void start(String agentPath, Instrumentation inst) throws Exception {
        PluginDependencyManager.initialize();

        // init log
        DOMConfigurator.configure(agentPath + separator + AgentContext.CONF_DIR + separator + CONF_LOG_FILE);

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        ensureApplicationTempDirectory(agentContext);

        // init setting
        AgentSettingManager.createInstance(agentContext.getAppInstance(),
                                           agentContext.getConfig().getFetcher());

        PluginInstaller.install(agentContext, inst);
    }

    private void ensureApplicationTempDirectory(AgentContext context) {
        File tmpDir = new File(context.getAgentDirectory() + separator + AgentContext.TMP_DIR + separator +
                               context.getConfig().getBootstrap().getAppName());

        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }
}
