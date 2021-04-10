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
