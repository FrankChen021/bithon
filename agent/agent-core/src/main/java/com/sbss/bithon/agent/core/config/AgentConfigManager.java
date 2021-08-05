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
import shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;
import shaded.com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;

import static com.sbss.bithon.agent.core.context.AgentContext.CONF_DIR;
import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 3:19 下午
 */
public class AgentConfigManager {

    public static final String BITHON_APPLICATION_ENV = "bithon.application.env";
    public static final String BITHON_APPLICATION_NAME = "bithon.application.name";
    private final File configFile;
    private static AgentConfigManager INSTANCE = null;

    public static AgentConfigManager createInstance(String agentDirectory) {
        INSTANCE = new AgentConfigManager(agentDirectory);
        return INSTANCE;
    }

    public static AgentConfigManager getInstance() {
        return INSTANCE;
    }

    private AgentConfigManager(String agentDirectory) {
        String conf = System.getProperty("bithon.conf");

        if (StringUtils.isEmpty(conf)) {
            configFile = new File(agentDirectory + separator + CONF_DIR + separator + "agent.yml");
        } else {
            configFile = new File(conf);
        }

        //
        // read properties from environment variables
        //

        //
        // read properties from java application arguments
        //
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-Dbithon.")) {
                String nameAndValue = arg.substring(2);

                // process default value
            }
        }

        // override properties in file
    }

    public <T> T getConfig(Class<T> clazz) throws IOException {
        return load(configFile, clazz);
    }

    public AgentConfig getAgentConfig() throws IOException {
        AgentConfig config = load(configFile, AgentConfig.class);

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

    private static <T> T load(File yml,
                              Class<T> clazz) throws IOException {
        try (InputStream inputStream = new FileInputStream(yml)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            return mapper.readValue(inputStream, clazz);
        }
    }
}
