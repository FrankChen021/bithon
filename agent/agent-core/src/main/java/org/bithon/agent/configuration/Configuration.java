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

package org.bithon.agent.configuration;

import org.bithon.agent.configuration.validation.Validator;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializationFeature;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.NullNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:13
 */
public class Configuration {

    private final JsonNode configurationNode;

    public static Configuration from(String configFileFormat, InputStream configStream) {
        return new Configuration(readStaticConfiguration(configFileFormat, configStream));
    }

    public Configuration(JsonNode configurationNode) {
        this.configurationNode = configurationNode;
    }

    /**
     * @return NotNull object
     */
    public static Configuration create(String configFileFormat,
                                       InputStream staticConfig,
                                       String dynamicPropertyPrefix,
                                       String... environmentVariables) {
        JsonNode staticConfiguration = readStaticConfiguration(configFileFormat, staticConfig);
        JsonNode dynamicConfiguration = readConfigurationFromArgs(dynamicPropertyPrefix, environmentVariables);
        return new Configuration(mergeNodes(staticConfiguration, dynamicConfiguration, false));
    }

    /**
     * Merge two configuration nodes into one recursively
     */
    private static JsonNode mergeNodes(JsonNode to, JsonNode from, boolean isReplace) {
        if (from == null) {
            return to;
        }

        Iterator<String> names = from.fieldNames();
        while (names.hasNext()) {

            String fieldName = names.next();
            JsonNode targetNode = to.get(fieldName);
            JsonNode sourceNode = from.get(fieldName);

            if (targetNode == null) {
                ((ObjectNode) to).set(fieldName, sourceNode);
                continue;
            }

            if (targetNode.isObject()) {
                // to json node exists, and it's an object, recursively merge
                mergeNodes(targetNode, sourceNode, isReplace);
            } else if (targetNode.isArray()) {
                if (sourceNode.isArray()) {
                    // merge arrays
                    ArrayNode sourceArray = (ArrayNode) sourceNode;

                    // Insert source nodes at the beginning of the target node.
                    // This makes the source nodes higher priority
                    if (isReplace) {
                        ((ArrayNode) targetNode).removeAll();
                        ((ArrayNode) targetNode).addAll(sourceArray);
                    } else {
                        for (int i = 0; i < sourceArray.size(); i++) {
                            ((ArrayNode) targetNode).insert(i, sourceArray.get(i));
                        }
                    }
                } else {
                    // use the source node to replace the 'to' node
                    ((ObjectNode) to).set(fieldName, sourceNode);
                }
            } else {
                // use the source node to replace the 'to' node
                ((ObjectNode) to).set(fieldName, sourceNode);
            }
        }

        return to;
    }

    private static JsonNode readStaticConfiguration(String configFileFormat, InputStream configStream) {
        if (configStream == null) {
            return new ObjectNode(new JsonNodeFactory(true));
        }

        ObjectMapper mapper;
        if (configFileFormat.endsWith(".yaml") || configFileFormat.endsWith(".yml")) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else if (configFileFormat.endsWith(".properties")) {
            mapper = new JavaPropsMapper();
        } else if (configFileFormat.endsWith(".json")) {
            mapper = new ObjectMapper();
        } else {
            throw new AgentException("Unknown property file type: %s", configFileFormat);
        }

        try {
            return ObjectMapperConfigurer.configure(mapper)
                                         .readTree(configStream);
        } catch (IOException e) {
            throw new AgentException("Failed to read property from static file[%s]:%s",
                                     configFileFormat,
                                     e.getMessage());
        }
    }

    private static JsonNode readConfigurationFromArgs(String dynamicPropertyPrefix,
                                                      String[] environmentVariables) {
        Map<String, String> userPropertyMap = new LinkedHashMap<>();

        //
        // read properties from environment variables.
        // environment variables have the lowest priority
        //
        if (environmentVariables != null) {
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

        try {
            return ObjectMapperConfigurer.configure(mapper)
                                         .readTree(userProperties.toString());
        } catch (IOException e) {
            throw new AgentException("Failed to read property user configuration:%s",
                                     e.getMessage());
        }
    }

    public void merge(Configuration configuration) {
        mergeNodes(this.configurationNode, configuration.configurationNode, false);
    }

    public void replace(Configuration configuration) {
        mergeNodes(this.configurationNode, configuration.configurationNode, true);
    }

    public boolean isEmpty() {
        return this.configurationNode.isEmpty();
    }

    /**
     * check if the configuration contains only the properties specified by given {@param pathPrefix}.
     */
    public boolean validate(String pathPrefix) {
        String[] paths = pathPrefix.split("\\.");
        JsonNode node = this.configurationNode;
        for (String path : paths) {
            if (node.size() > 1) {
                return false;
            }
            node = node.get(path);
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    public Set<String> getKeys() {
        Set<String> result = new HashSet<>();
        getKeys(result, new ArrayList<>(), this.configurationNode);
        return result;
    }

    private void getKeys(Set<String> result, List<String> path, JsonNode node) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String fieldName = names.next();
            JsonNode targetNode = node.get(fieldName);

            int addIndex = path.size();
            path.add(fieldName);

            if (targetNode.isObject()) {
                getKeys(result, path, targetNode);
            } else {
                // use the source node to replace the 'to' node
                result.add(String.join(".", path));
            }
            path.remove(addIndex);
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
        return getConfig(prefixes,
                         clazz,
                         () -> {
                             // default value provider
                             try {
                                 if (clazz == Boolean.class) {
                                     //noinspection unchecked
                                     return (T) Boolean.FALSE;
                                 }
                                 if (clazz.isArray()) {
                                     return (T) Array.newInstance(clazz.getComponentType(), 0);
                                 }
                                 Constructor<T> ctor = clazz.getDeclaredConstructor();
                                 ctor.setAccessible(true);
                                 return ctor.newInstance();
                             } catch (IllegalAccessException e) {
                                 throw new AgentException("Unable create instance for [%s]: %s", clazz.getName(), e.getMessage());
                             } catch (NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                                 throw new AgentException("Unable create instance for [%s]: %s",
                                                          clazz.getName(),
                                                          e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
                             }
                         });
    }

    public <T> T getConfig(String prefixes, Class<T> clazz, Supplier<T> defaultSupplier) {
        JsonNode node = configurationNode;

        // Find the correct node by prefixes
        for (String prefix : prefixes.split("\\.")) {
            if (node == null) {
                break;
            }
            node = node.get(prefix);
        }

        if (node == null || node instanceof NullNode) {
            return defaultSupplier.get();
        }
        return getConfig(prefixes, node, clazz);
    }

    private <T> T getConfig(String prefixes, JsonNode configurationNode, Class<T> clazz) {
        T value;
        try {
            value = ObjectMapperConfigurer.configure(new ObjectMapper())
                                          .convertValue(configurationNode, clazz);
        } catch (IllegalArgumentException e) {
            throw new AgentException(e,
                                     "Unable to read type of [%s] from configuration: %s",
                                     clazz.getSimpleName(),
                                     e.getMessage());
        }

        String violation = Validator.validate(prefixes, value);
        if (violation != null) {
            throw new AgentException("Invalid configuration for type of [%s]: %s",
                                     clazz.getSimpleName(),
                                     violation);
        }

        return value;
    }

    public String format(String format, boolean prettyFormat) {
        ObjectMapper mapper;
        if ("yaml".equals(format) || "yml".equals(format)) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else if ("properties".equals(format)) {
            mapper = new JavaPropsMapper();
        } else if ("json".equals(format)) {
            mapper = new ObjectMapper();
        } else {
            throw new AgentException("Unknown format: %s", format);
        }

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        if (prettyFormat) {
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        }
        try {
            return mapper.writeValueAsString(this.configurationNode);
        } catch (JsonProcessingException e) {
            throw new AgentException(e);
        }
    }
}
