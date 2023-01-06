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

package org.bithon.agent.core.config;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.component.commons.utils.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/5 22:20
 */
public class AgentConfiguration {

    private static AgentConfiguration INSTANCE;

    public AgentConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public static AgentConfiguration getInstance() {
        return INSTANCE;
    }

    public static AgentConfiguration create(String agentDirectory) {
        File staticConfig = new File(agentDirectory + separator + AgentContext.CONF_DIR + separator + "agent.yml");
        try (FileInputStream is = new FileInputStream(staticConfig)) {
            INSTANCE = new AgentConfiguration(Configuration.create(staticConfig.getAbsolutePath(),
                                                                   is,
                                                                   "bithon.",
                                                                   AgentContext.BITHON_APPLICATION_NAME,
                                                                   AgentContext.BITHON_APPLICATION_ENV));
            return INSTANCE;
        } catch (FileNotFoundException e) {
            throw new AgentException("Unable to find static config at [%s]", staticConfig.getAbsolutePath());
        } catch (IOException e) {
            throw new AgentException("Unexpected IO exception occurred: %s", e.getMessage());
        }
    }

    private final Configuration configuration;

    public <T> T getConfig(Class<T> clazz) {
        ConfigurationProperties cfg = clazz.getAnnotation(ConfigurationProperties.class);
        if (cfg != null && !StringUtils.isEmpty(cfg.prefix())) {
            return getConfig(cfg.prefix(), clazz);
        } else {
            throw new AgentException("Class [%s] does not have valid ConfigurationProperties.", clazz.getName());
        }
    }

    public <T> T getConfig(String prefixes, Class<T> clazz) {
        return configuration.getConfig(prefixes, clazz);
    }

    public void addConfiguration(Configuration pluginConfiguration) {
        configuration.merge(pluginConfiguration);
    }
}
