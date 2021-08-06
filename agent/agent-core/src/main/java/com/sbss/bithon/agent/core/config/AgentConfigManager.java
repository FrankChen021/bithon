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
import shaded.com.fasterxml.jackson.databind.JsonNode;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;
import shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import shaded.com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import shaded.com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.sbss.bithon.agent.core.context.AgentContext.CONF_DIR;
import static java.io.File.separator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 3:19 下午
 */
public class AgentConfigManager {

    public static final String BITHON_APPLICATION_ENV = "bithon.application.env";
    public static final String BITHON_APPLICATION_NAME = "bithon.application.name";
    private final JsonNode configuration;
    private static AgentConfigManager INSTANCE = null;

    public static AgentConfigManager createInstance(String agentDirectory) {
        INSTANCE = new AgentConfigManager(agentDirectory);
        return INSTANCE;
    }

    public static AgentConfigManager getInstance() {
        return INSTANCE;
    }

    private AgentConfigManager(String agentDirectory) {
        File configFile = new File(agentDirectory + separator + CONF_DIR + separator + "agent.yml");

        JsonNode staticConfiguration = readStaticConfiguration(configFile);
        JsonNode dynamicConfiguration = readDynamicConfiguration();
        configuration = mergeConfiguration(staticConfiguration, dynamicConfiguration);
    }

    private JsonNode mergeConfiguration(JsonNode target, JsonNode source) {
        if (source == null) {
            return target;
        }

        Iterator<String> names = source.fieldNames();
        while (names.hasNext()) {

            String fieldName = names.next();
            JsonNode targetNode = target.get(fieldName);

            if (targetNode != null && targetNode.isObject()) {
                // target json node exists, and it's an object, recursively merge
                mergeConfiguration(targetNode, source.get(fieldName));
            } else {
                // target node does not exist or target node is not an object
                //
                // check if target is an object so that we could do a safe conversion
                if (target instanceof ObjectNode) {
                    // Overwrite field
                    JsonNode value = source.get(fieldName);
                    ((ObjectNode) target).set(fieldName, value);
                }
            }

        }

        return target;
    }

    private JsonNode readStaticConfiguration(File configFile) {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            return mapper.readTree(configFile);
        } catch (IOException e) {
            throw new AgentException("Failed to read property from file[%s]:%s",
                                     configFile.getAbsolutePath(),
                                     e.getMessage());
        }
    }

    private JsonNode readDynamicConfiguration() {
        Map<String, String> userPropertyMap = new LinkedHashMap<>();

        //
        // read properties from environment variables.
        // environment variables have the lowest priority
        //
        String[] envPropertyNames = new String[]{BITHON_APPLICATION_ENV, BITHON_APPLICATION_NAME};
        for (String envName : envPropertyNames) {
            String envValue = System.getenv(envName);
            if (!StringUtils.isEmpty(envValue)) {
                userPropertyMap.put(envName.substring("bithon.".length()), envValue);
            }
        }

        //
        // read properties from java application arguments
        //
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-Dbithon.")) {
                continue;
            }

            String nameAndValue = arg.substring("-Dbithon.".length());
            if (StringUtils.isEmpty(nameAndValue)) {
                continue;
            }

            int assignmentIndex = nameAndValue.indexOf('=');
            if (assignmentIndex == -1) {
                continue;
            }
            userPropertyMap.put(nameAndValue.substring(0, assignmentIndex),
                                nameAndValue.substring(assignmentIndex + 1));
        }

        StringBuilder userProperties = new StringBuilder();
        for (Map.Entry<String, String> entry : userPropertyMap.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            userProperties.append(name);
            userProperties.append('=');
            userProperties.append(value);
            userProperties.append('\n');
        }
        JavaPropsMapper mapper = new JavaPropsMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        try {
            return mapper.readTree(userProperties.toString());
        } catch (IOException e) {
            throw new AgentException("Failed to read property user configuration:%s",
                                     e.getMessage());
        }
    }

    public <T> T getConfig(Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        return mapper.convertValue(configuration, clazz);
    }

    public AgentConfig getAgentConfig() throws IOException {
        AgentConfig config = getConfig(AgentConfig.class);

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
}
