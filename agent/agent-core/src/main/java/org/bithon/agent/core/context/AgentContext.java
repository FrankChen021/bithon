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

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.config.Configuration;
import org.bithon.agent.core.config.ConfigurationProperties;
import org.bithon.agent.core.config.validation.NotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 2:18 下午
 */
public class AgentContext {

    @ConfigurationProperties(prefix = "application")
    public static class AppConfiguration {

        @NotBlank(message = "'bithon.application.env' should not be blank")
        private String env;

        @NotBlank(message = "'bithon.application.name' should not be blank")
        private String name;

        /**
         * This is not for configuration.
         * In most cases, this field is automatically retrieved by the agent.
         * But if the applications is deployed in Docker on different host, the container ip may be the same.
         * In such case, user can use this configuration to override the automatically retrieved instance name.
         */
        private String instance;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEnv() {
            return env;
        }

        public void setEnv(String env) {
            this.env = env;
        }

        public String getInstance() {
            return instance;
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }
    }

    public static final String BITHON_APPLICATION_ENV = "bithon.application.env";
    public static final String BITHON_APPLICATION_NAME = "bithon.application.name";

    public static final String CONF_DIR = "conf";
    public static final String TMP_DIR = "tmp";
    private static AgentContext INSTANCE;
    private String agentDirectory;
    private AppInstance appInstance;
    private Configuration agentConfiguration;

    public static AgentContext createInstance(String agentPath) {
        Configuration configuration = loadAgentConfiguration(agentPath);
        AppConfiguration appConfiguration = configuration.getConfig(AppConfiguration.class);

        INSTANCE = new AgentContext();
        INSTANCE.agentDirectory = agentPath;
        INSTANCE.agentConfiguration = configuration;
        INSTANCE.appInstance = new AppInstance(appConfiguration);
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

    public Configuration getAgentConfiguration() {
        return agentConfiguration;
    }

    private static Configuration loadAgentConfiguration(String agentDirectory) {
        File staticConfig = new File(agentDirectory + separator + CONF_DIR + separator + "agent.yml");
        try (FileInputStream is = new FileInputStream(staticConfig)) {
            return Configuration.create(staticConfig.getAbsolutePath(),
                                        is,
                                        "bithon.",
                                        BITHON_APPLICATION_NAME,
                                        BITHON_APPLICATION_ENV);
        } catch (FileNotFoundException e) {
            throw new AgentException("Unable to find static config at [%s]", staticConfig.getAbsolutePath());
        } catch (IOException e) {
            throw new AgentException("Unexpected IO exception occurred: %s", e.getMessage());
        }
    }
}
