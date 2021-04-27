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
import com.sbss.bithon.agent.core.utils.StringUtils;
import com.sbss.bithon.agent.core.utils.YamlUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author frankchen
 * @date 2020-12-31 22:18:18
 */
public class AgentConfig {
    public static final String BITHON_APPLICATION_ENV = "bithon.application.env";
    public static final String BITHON_APPLICATION_NAME = "bithon.application.name";
    private boolean traceEnabled = true;
    private BootstrapConfig bootstrap;
    private Map<String, DispatcherConfig> dispatchers;
    private FetcherConfig fetcher;

    public static AgentConfig loadFromYmlFile(String defaultFilePath) throws IOException {
        String conf = System.getProperty("conf");

        String configFile = !StringUtils.isEmpty(conf) ? conf : defaultFilePath;

        AgentConfig config = YamlUtils.load(new File(configFile), AgentConfig.class);

        String appName = getApplicationName(config.getBootstrap().getAppName());
        if (StringUtils.isEmpty(appName)) {
            throw new AgentException("Failed to get JVM property or environment variable `%s`",
                                     BITHON_APPLICATION_NAME);
        }
        config.getBootstrap().setAppName(appName);

        String env = getApplicationEnvironment();
        if (StringUtils.isEmpty(env)) {
            throw new AgentException("Failed to get JVM property or environment variable `%s`", BITHON_APPLICATION_ENV);
        }
        config.getBootstrap().setEnv(env);

        return config;
    }

    private static String getApplicationName(String defaultName) {
        String appName = System.getProperty(BITHON_APPLICATION_NAME);
        if (!StringUtils.isEmpty(appName)) {
            return appName;
        }

        appName = System.getenv().get(BITHON_APPLICATION_NAME);
        if (!StringUtils.isEmpty(appName)) {
            return appName;
        }

        return defaultName;
    }

    private static String getApplicationEnvironment() {
        String envName = System.getProperty(BITHON_APPLICATION_ENV);
        if (!StringUtils.isEmpty(envName)) {
            return envName;
        }

        envName = System.getenv().get(BITHON_APPLICATION_ENV);
        if (!StringUtils.isEmpty(envName)) {
            return envName;
        }

        return null;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public FetcherConfig getFetcher() {
        return fetcher;
    }

    public void setFetcher(FetcherConfig fetcher) {
        this.fetcher = fetcher;
    }

    public BootstrapConfig getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(BootstrapConfig bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Map<String, DispatcherConfig> getDispatchers() {
        return dispatchers;
    }

    public void setDispatchers(Map<String, DispatcherConfig> dispatchers) {
        this.dispatchers = dispatchers;
    }
}
