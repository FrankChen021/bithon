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

package org.bithon.agent.core.context;

import org.bithon.agent.core.config.AppConfiguration;
import org.bithon.agent.core.config.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 2:18 下午
 */
public class AgentContext {

    public static final String BITHON_APPLICATION_ENV = "bithon.application.env";
    public static final String BITHON_APPLICATION_NAME = "bithon.application.name";

    public static final String CONF_DIR = "conf";
    public static final String TMP_DIR = "tmp";
    private static AgentContext INSTANCE;
    private String agentDirectory;
    private AppInstance appInstance;

    public static AgentContext createInstance(String agentPath, Configuration agentConfiguration) {
        INSTANCE = new AgentContext();
        INSTANCE.agentDirectory = agentPath;
        INSTANCE.appInstance = new AppInstance(agentConfiguration.getConfig(AppConfiguration.class));
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
}
