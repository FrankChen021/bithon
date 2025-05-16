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

package org.bithon.agent.config;

import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.configuration.validation.NotBlank;
import org.bithon.agent.configuration.validation.RegExpr;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/5 22:30
 */
@ConfigurationProperties(path = "application", dynamic = false)
public class AppConfig {

    @NotBlank(message = "'bithon.application.env' should not be blank")
    @RegExpr(expr = "^[\\w_-]+$", message = "The 'bithon.application.env' [%s] is illegally configured. Only letter/digit/underscore/hyphen is allowed")
    private String env;

    @NotBlank(message = "'bithon.application.name' should not be blank")
    @RegExpr(expr = "^[\\w_-]+$", message = "The 'bithon.application.name' [%s] is illegally configured. Only letter/digit/underscore/hyphen is allowed")
    private String name;

    /**
     * For non-web application, the port can't be detected automatically.
     * In this case, to make the agent work, the port needs to be specified manually.
     */
    private int port = 0;

    /**
     * Indicate the name of the monitored application instance.
     * By default, the agent will generate the instance name automatically by using the current host ip address.
     * There are two cases that user might want to specify the instance name manually.
     * <p>
     * Case 1: If the application is deployed in Docker on different host, the container ip may be the same.
     * In such a case, user can use this configuration to override the automatically retrieved instance name.
     *
     * Case 2: If application is deployed in K8S, if you want to use the pod name as the instance name, you can set this configuration.
     */
    private String instance;

    /**
     * Some application forks subprocesses, and if the host application has bithon_application_instance configured,
     * subprocesses will inherit the instance name from the parent process.
     * This causes two different processes to share the same instance name,
     * and it's NOT able to tell the difference from monitoring.
     *
     * To solve such a problem,
     * subprocesses can define this configuration
     * to force using generated instance name instead of bithon_application_instance.
     *
     * The default value of this field is set to 'false' to keep backward compatibility.
     */
    private boolean noUseExternalInstanceName = false;

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

    public boolean isNoUseExternalInstanceName() {
        return noUseExternalInstanceName;
    }

    public void setNoUseExternalInstanceName(boolean noUseExternalInstanceName) {
        this.noUseExternalInstanceName = noUseExternalInstanceName;
    }
}
