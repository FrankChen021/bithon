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
import org.bithon.agent.core.config.validation.Validator;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:13
 */
public class Configuration {

    public static final Configuration EMPTY = new Configuration(null);

    private final JsonNode configurationNode;

    public Configuration(JsonNode configurationNode) {
        this.configurationNode = configurationNode;
    }

    public static Configuration create(String location,
                                       InputStream staticConfig,
                                       String dynamicPropertyPrefix,
                                       String... environmentVariables) {
        JsonNode staticConfiguration = readStaticConfiguration(location, staticConfig);
        JsonNode dynamicConfiguration = readDynamicConfiguration(dynamicPropertyPrefix, environmentVariables);
        return new Configuration(mergeConfiguration(staticConfiguration, dynamicConfiguration));
    }

    private static JsonNode mergeConfiguration(JsonNode target, JsonNode source) {
        if (source == null) {
            return target;
        }

        Iterator<String> names = source.fieldNames();
        while (names.hasNext()) {

            String fieldName = names.next();
            JsonNode targetNode = target.get(fieldName);
            JsonNode sourceNode = source.get(fieldName);

            if (targetNode == null) {
                ((ObjectNode) target).set(fieldName, sourceNode);
                continue;
            }

            if (targetNode.isObject()) {
                // target json node exists, and it's an object, recursively merge
                mergeConfiguration(targetNode, sourceNode);
            } else if (targetNode.isArray()) {
                if (sourceNode.isArray()) {
                    // merge arrays
                    ArrayNode sourceArray = (ArrayNode) sourceNode;

                    // insert source nodes at the beginning of the target node
                    // this makes the source nodes higher priority
                    for (int i = 0; i < sourceArray.size(); i++) {
                        ((ArrayNode) targetNode).insert(i, sourceArray.get(i));
                    }
                } else {
                    // use the source node to replace the target node
                    ((ObjectNode) target).set(fieldName, sourceNode);
                }
            } else {
                // use the source node to replace the target node
                ((ObjectNode) target).set(fieldName, sourceNode);
            }
        }

        return target;
    }

    private static JsonNode readStaticConfiguration(String location, InputStream configFile) {
        if (configFile == null) {
            return new ObjectNode(new JsonNodeFactory(true));
        }

        ObjectMapper mapper;
        if (location.endsWith(".yaml") || location.endsWith(".yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else if (location.endsWith(".properties")) {
            mapper = new JavaPropsMapper();
        } else if (location.endsWith(".json")) {
            mapper = new ObjectMapper();
        } else {
            throw new AgentException("Unknown property file type: %s", location);
        }

        try {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            return mapper.readTree(configFile);
        } catch (IOException e) {
            throw new AgentException("Failed to read property from static file[%s]:%s",
                                     location,
                                     e.getMessage());
        }
    }

    private static JsonNode readDynamicConfiguration(String dynamicPropertyPrefix,
                                                     String[] environmentVariables) {
        Map<String, String> userPropertyMap = new LinkedHashMap<>();

        //
        // read properties from environment variables.
        // environment variables have the lowest priority
        //
        if (environmentVariables != null && environmentVariables.length > 0) {
            for (String envName : environmentVariables) {
                String envValue = System.getenv(envName);
                if (!StringUtils.isEmpty(envValue)) {
                    userPropertyMap.put(envName.substring("bithon.".length()), envValue);
                }
            }
        }

        //
        // read properties from java application arguments
        //
        String applicationArg = "-D" + dynamicPropertyPrefix;
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith(applicationArg) || !arg.startsWith("-Dbithon.")) {
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

    public <T> T getConfig(Class<T> clazz) {
        ConfigurationProperties cfg = clazz.getAnnotation(ConfigurationProperties.class);
        if (cfg != null && !StringUtils.isEmpty(cfg.prefix())) {
            return getConfig(cfg.prefix(), clazz);
        } else {
            return getConfig("[root]", clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String prefixes, Class<T> clazz) {
        return getConfig(prefixes, clazz, () -> {
            try {
                if (clazz == Boolean.class) {
                    //noinspection unchecked
                    return (T) Boolean.FALSE;
                }
                return clazz.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException e) {
                throw new AgentException("Unable create instance for [%s]: %s", clazz.getName(), e.getMessage());
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                throw new AgentException("Unable create instance for [%s]: %s",
                                         clazz.getName(),
                                         e.getCause().getMessage());
            }
        });
    }

    public <T> T getConfig(String prefixes, Class<T> clazz, Supplier<T> defaultSupplier) {
        JsonNode node = configurationNode;

        // find correct node by prefixes
        for (String prefix : prefixes.split("\\.")) {
            if (node == null) {
                break;
            }
            node = node.get(prefix);
        }

        if (node == null) {
            return defaultSupplier.get();
        }
        return getConfig(node, clazz);
    }

    private <T> T getConfig(JsonNode configurationNode, Class<T> clazz) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        T value;
        try {
            value = mapper.convertValue(configurationNode, clazz);
        } catch (IllegalArgumentException e) {
            throw new AgentException(e,
                                     "Unable to read type of [%s] from configuration: %s",
                                     clazz.getSimpleName(),
                                     e.getMessage());
        }

        String violation = Validator.validate(value);
        if (violation != null) {
            throw new AgentException("Invalid configuration for type of [%s]: %s",
                                     clazz.getSimpleName(),
                                     violation);
        }

        return value;
    }
}
