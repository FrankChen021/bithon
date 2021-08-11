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

package com.sbss.bithon.agent.core.config;

import com.sbss.bithon.agent.bootstrap.expt.AgentException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.sbss.bithon.agent.core.context.AgentContext.CONF_DIR;
import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 3:19 下午
 */
public class AgentConfigManager {

    public static final String BITHON_APPLICATION_ENV = "bithon.application.env";
    public static final String BITHON_APPLICATION_NAME = "bithon.application.name";

    private static ConfigManager INSTANCE = null;

    public static ConfigManager createInstance(String agentDirectory) {
        File staticConfig = new File(agentDirectory + separator + CONF_DIR + separator + "agent.yml");
        try (FileInputStream is = new FileInputStream(staticConfig)) {
            INSTANCE = ConfigManager.create(staticConfig.getAbsolutePath(),
                                            is,
                                            "bithon.",
                                            BITHON_APPLICATION_NAME,
                                            BITHON_APPLICATION_ENV);
            return INSTANCE;
        } catch (FileNotFoundException e) {
            throw new AgentException("Unable to find static config at [%s]", staticConfig.getAbsolutePath());
        } catch (IOException e) {
            throw new AgentException("Unexpected IO exception occurred: %s", e.getMessage());
        }
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }
}
