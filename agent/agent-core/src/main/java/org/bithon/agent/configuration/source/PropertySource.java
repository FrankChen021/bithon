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

package org.bithon.agent.configuration.source;

import org.bithon.agent.configuration.ConfigurationFormat;
import org.bithon.agent.configuration.ObjectMapperConfigurer;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializationFeature;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:13
 */
public class PropertySource implements Comparable<PropertySource> {

    private Object tag;
    private final String name;
    private final PropertySourceType type;
    private JsonNode propertyNode;

    /**
     * Create an empty configuration
     */
    public PropertySource(PropertySourceType type, String name) {
        this(type, name, new ObjectNode(new JsonNodeFactory(true)));
    }

    public PropertySource(PropertySourceType type, String name, JsonNode propertyNode) {
        this.type = type;
        this.name = name;
        this.propertyNode = propertyNode;
    }

    public static PropertySource from(PropertySourceType source, String name, String propertyText) throws IOException {
        return new PropertySource(source, name,
                                  ObjectMapperConfigurer.configure(new JavaPropsMapper())
                                                        .readTree(propertyText));
    }

    public static PropertySource from(PropertySourceType source, File configFilePath, boolean checkFileExists) {
        try (FileInputStream fs = new FileInputStream(configFilePath)) {
            ConfigurationFormat fileFormat = ConfigurationFormat.determineFormatFromFile(configFilePath.getName());
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

    public static PropertySource from(PropertySourceType source,
                                      String name,
                                      ConfigurationFormat configurationFormat,
                                      InputStream configStream) {
        if (configStream == null) {
            // Returns an empty one
            return new PropertySource(source, name);
        }

        try {
            return new PropertySource(source,
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
    public static JsonNode merge(JsonNode target, JsonNode source, boolean isReplace) {
        if (source == null) {
            return target;
        }

        if (target instanceof ArrayNode && source instanceof ArrayNode) {
            if (isReplace) {
                ((ArrayNode) target).removeAll();
            }
            ((ArrayNode) target).addAll((ArrayNode) source);
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
                    ((ObjectNode) target).set(fieldName, sourceNode);
                }
            } else {
                // use the source node to replace the 'to' node
                ((ObjectNode) target).set(fieldName, sourceNode);
            }
        }

        return target;
    }

    public PropertySourceType getType() {
        return type;
    }

    public String getName() {
        return this.name;
    }

    public void swap(PropertySource propertySource) {
        {
            JsonNode tmp = this.propertyNode;
            this.propertyNode = propertySource.propertyNode;
            propertySource.propertyNode = tmp;
        }
        {
            Object tmp = this.tag;
            this.tag = propertySource.tag;
            propertySource.tag = tmp;
        }
    }

    public PropertySource merge(PropertySource propertySource) {
        if (propertySource != null) {
            merge(this.propertyNode, propertySource.propertyNode, false);
        }
        return this;
    }

    public boolean isEmpty() {
        return this.propertyNode.isEmpty();
    }

    public JsonNode getPropertyNode(String[] paths) {
        JsonNode node = this.propertyNode;
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
        return this.getPropertyNode(property.split("\\.")) != null;
    }

    public Set<String> getKeys() {
        Set<String> result = new HashSet<>();
        getKeys(result, new ArrayList<>(), this.propertyNode);
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

    /**
     * deep clone
     */
    @Override
    public PropertySource clone() {
        return new PropertySource(type, name, propertyNode.deepCopy());
    }

    @Override
    public String toString() {
        return "type=" + this.type + ", name=" + this.name;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    @Override
    public int compareTo(PropertySource o) {
        if (this.type.priority() == o.type.priority()) {
            return this.name.compareTo(o.name);
        } else {
            return this.type.priority() - o.type.priority();
        }
    }

    public String format(String format, boolean prettyFormat) {
        try {
            return ConfigurationFormat.determineFormat(format)
                                      .createMapper()
                                      .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                                      .configure(SerializationFeature.INDENT_OUTPUT, prettyFormat)
                                      .writeValueAsString(this.propertyNode);
        } catch (JsonProcessingException e) {
            throw new AgentException(e);
        }
    }
}
