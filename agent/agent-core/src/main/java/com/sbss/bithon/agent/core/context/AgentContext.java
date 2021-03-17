package com.sbss.bithon.agent.core.context;

import com.sbss.bithon.agent.core.config.AgentConfig;

import java.io.IOException;

import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 2:18 下午
 */
public class AgentContext {

    public static final String CONF_DIR = "conf";
    public static final String PLUGIN_DIR = "plugins";
    public static final String TMP_DIR = "tmp";
    private static AgentContext INSTANCE;
    private String agentDirectory;
    private AppInstance appInstance;
    private AgentConfig agentConfig;

    public static AgentContext createInstance(String agentPath) throws IOException {
        AgentConfig config = AgentConfig.loadFromYmlFile(agentPath + separator + CONF_DIR + separator + "agent.yml");

        INSTANCE = new AgentContext();
        INSTANCE.agentDirectory = agentPath;
        INSTANCE.agentConfig = config;
        INSTANCE.appInstance = new AppInstance(config.getBootstrap().getAppName(), config.getBootstrap().getEnv());
        return INSTANCE;
    }

    public static AgentContext getInstance() {
        return INSTANCE;
    }

    public String getAgentDirectory() {
        return agentDirectory;
    }

    public AppInstance getAppInstance() {
        return appInstance;
    }

    public AgentConfig getConfig() {
        return agentConfig;
    }

}
