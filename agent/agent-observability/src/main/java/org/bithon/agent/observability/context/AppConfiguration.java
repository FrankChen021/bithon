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

package org.bithon.agent.observability.context;

import org.bithon.agent.core.config.ConfigurationProperties;
import org.bithon.agent.core.config.validation.NotBlank;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/5 22:30
 */
@ConfigurationProperties(prefix = "application", dynamic = false)
public class AppConfiguration {

    @NotBlank(message = "'bithon.application.env' should not be blank")
    private String env;

    @NotBlank(message = "'bithon.application.name' should not be blank")
    private String name;

    /**
     * For non web application, the port can't be detected automatically.
     * In this case in order to make the agent work, the port needs to be specified manually.
     */
    private int port = 0;

    /**
     * Indicate the host and port of the monitored application instance.
     * In the format of ip:port
     * <p>
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
