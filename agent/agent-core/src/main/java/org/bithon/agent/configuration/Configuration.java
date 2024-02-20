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

import org.bithon.agent.configuration.source.ConfigurationSource;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:13
 */
public class Configuration implements Comparable<Configuration> {

    private Object tag;
    private final String name;
    private final ConfigurationSource source;
    private JsonNode configurationNode;

    /**
     * Create an empty configuration
     */
    public Configuration(ConfigurationSource source, String name) {
        this(source, name, new ObjectNode(new JsonNodeFactory(true)));
    }

    protected Configuration(ConfigurationSource source, String name, JsonNode configurationNode) {
        this.source = source;
        this.name = name;
        this.configurationNode = configurationNode;
    }

    public static Configuration from(ConfigurationSource source, String name, String propertyText) throws IOException {
        return new Configuration(source, name,
                                 ObjectMapperConfigurer.configure(new JavaPropsMapper())
                                                       .readTree(propertyText));
    }

    public static Configuration from(ConfigurationSource source, File configFilePath, boolean checkFileExists) {
        ConfigurationFormat fileFormat = ConfigurationFormat.determineFormatFromFile(configFilePath.getName());
        try (FileInputStream fs = new FileInputStream(configFilePath)) {
            return from(source, configFilePath.getName(), fileFormat, fs);
        } catch (FileNotFoundException e) {
            if (checkFileExists) {
                throw new AgentException("Unable to find config file at [%s]", configFilePath);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new AgentException("Unexpected IO exception occurred: %s", e.getMessage());
        }
    }

    public static Configuration from(ConfigurationSource source,
                                     String name,
                                     ConfigurationFormat configurationFormat,
                                     InputStream configStream) {
        if (configStream == null) {
            // Returns an empty one
            return new Configuration(source, name);
        }

        try {
            return new Configuration(source,
                                     name,
                                     ObjectMapperConfigurer.configure(configurationFormat.createMapper())
                                                           .readTree(configStream));
        } catch (IOException e) {
            throw new AgentException("Failed to read configuration from file [%s]: %s",
                                     configurationFormat,
                                     e.getMessage());
        }
    }

    /**
     * Merge two configuration nodes into one recursively
     */
    public static JsonNode merge(JsonNode to, JsonNode from, boolean isReplace) {
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
                merge(targetNode, sourceNode, isReplace);
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

    public ConfigurationSource getSource() {
        return source;
    }

    public String getName() {
        return this.name;
    }

    public void swap(Configuration configuration) {
        {
            JsonNode tmp = this.configurationNode;
            this.configurationNode = configuration.configurationNode;
            configuration.configurationNode = tmp;
        }
        {
            Object tmp = this.tag;
            this.tag = configuration.tag;
            configuration.tag = tmp;
        }
    }

    public Configuration merge(Configuration configuration) {
        if (configuration != null) {
            merge(this.configurationNode, configuration.configurationNode, false);
        }
        return this;
    }

    public boolean isEmpty() {
        return this.configurationNode.isEmpty();
    }

    public JsonNode getConfigurationNode(String[] paths) {
        JsonNode node = this.configurationNode;
        for (String path : paths) {
            if (node.isContainerNode()) {
                node = node.get(path);
                if (node == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return node;
    }

    /**
     * check if the configuration contains only the properties specified by given {@param pathPrefix}.
     */
    public boolean contains(String property) {
        return this.getConfigurationNode(property.split("\\.")) != null;
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
        if (cfg != null && !StringUtils.isEmpty(cfg.path())) {
            return getConfig(cfg.path(), clazz);
        } else {
            return getConfig("[root]", clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfig(String propertyPath, Class<T> clazz) {
        return getConfig(propertyPath,
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

    public <T> T getConfig(String propertyPath, Class<T> clazz, Supplier<T> defaultSupplier) {
        if (this.configurationNode.isEmpty()) {
            return defaultSupplier.get();
        }

        JsonNode node = configurationNode;

        // Find the correct node by prefix
        for (String part : propertyPath.split("\\.")) {
            if (node == null) {
                break;
            }
            node = node.get(part);
        }

        if (node == null || node instanceof NullNode) {
            return defaultSupplier.get();
        }

        return getConfig(propertyPath, node, clazz);
    }

    private <T> T getConfig(String propertyPath, JsonNode configuration, Class<T> clazz) {
        T value;
        try {
            value = ObjectMapperConfigurer.configure(new ObjectMapper())
                                          .convertValue(configuration, clazz);
        } catch (IllegalArgumentException e) {
            throw new AgentException(e,
                                     "Unable to read type of [%s] from configuration: %s",
                                     clazz.getSimpleName(),
                                     e.getMessage());
        }

        String violation = Validator.validate(propertyPath, value);
        if (violation != null) {
            throw new AgentException("Invalid configuration for type of [%s]: %s",
                                     clazz.getSimpleName(),
                                     violation);
        }

        return value;
    }

    /**
     * deep clone
     */
    @Override
    public Configuration clone() {
        return new Configuration(source, name, configurationNode.deepCopy());
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    @Override
    public int compareTo(Configuration o) {
        if (this.source.priority() == o.source.priority()) {
            return this.name.compareTo(o.name);
        } else {
            return this.source.priority() - o.source.priority();
        }
    }

    public String format(String format, boolean prettyFormat) {
        try {
            return ConfigurationFormat.determineFormat(format)
                                      .createMapper()
                                      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                                      .configure(SerializationFeature.INDENT_OUTPUT, prettyFormat)
                                      .writeValueAsString(this.configurationNode);
        } catch (JsonProcessingException e) {
            throw new AgentException(e);
        }
    }
}
