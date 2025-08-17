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

package org.bithon.agent.controller.config;

import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.bithon.shaded.com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class to extract default values from configuration classes at runtime.
 * This is used to populate default values in configuration metadata that couldn't
 * be determined at compile-time.
 *
 * @author frank.chen021@outlook.com
 */
public class ConfigurationDefaultValueExtractor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(ConfigurationDefaultValueExtractor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Cache of instantiated configuration classes to avoid recreating them
    private static final Map<String, Object> INSTANCE_CACHE = new ConcurrentHashMap<>();

    /**
     * Extract default values for a list of property metadata objects.
     * Groups properties by configuration class to minimize object creation.
     *
     * @param properties the list of property metadata to process
     */
    public static void extractDefaultValues(List<PropertyMetadata> properties) {
        // Group properties by configuration class
        Map<String, Map<String, PropertyMetadata>> propertiesByClass = new HashMap<>();

        for (PropertyMetadata property : properties) {
            String configClass = property.getConfigurationClass();
            if (configClass == null || configClass.isEmpty()) {
                continue;
            }

            propertiesByClass
                .computeIfAbsent(configClass, k -> new HashMap<>())
                .put(property.getPath(), property);
        }

        // Process each configuration class
        for (Map.Entry<String, Map<String, PropertyMetadata>> entry : propertiesByClass.entrySet()) {
            String configClass = entry.getKey();
            Map<String, PropertyMetadata> classProperties = entry.getValue();

            try {
                // Get or create an instance of the configuration class
                Object instance = getConfigClassInstance(configClass);
                if (instance == null) {
                    continue;
                }

                // Extract default values for all properties of this class
                extractDefaultValuesFromInstance(instance, classProperties);
            } catch (Exception e) {
                log.warn("Failed to extract default values for configuration class {}: {}",
                         configClass, e.getMessage());
            }
        }
    }

    /**
     * Get or create an instance of a configuration class.
     * Uses a cache to avoid recreating instances.
     *
     * @param className the fully qualified class name
     * @return an instance of the class, or null if instantiation fails
     */
    private static Object getConfigClassInstance(String className) {
        // Check cache first
        Object instance = INSTANCE_CACHE.get(className);
        if (instance != null) {
            return instance;
        }

        try {
            // Load the class
            Class<?> clazz = Class.forName(className);

            // Create an instance using the no-arg constructor
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();

            // Cache the instance
            INSTANCE_CACHE.put(className, instance);
            return instance;
        } catch (ClassNotFoundException e) {
            log.warn("Configuration class not found: {}", className);
        } catch (NoSuchMethodException e) {
            log.warn("No default constructor found for configuration class: {}", className);
        } catch (Exception e) {
            log.warn("Failed to instantiate configuration class {}: {}", className, e.getMessage());
        }

        return null;
    }

    /**
     * Extract default values from an instance of a configuration class.
     *
     * @param instance   the configuration class instance
     * @param properties map of property paths to PropertyMetadata objects
     */
    private static void extractDefaultValuesFromInstance(Object instance,
                                                         Map<String, PropertyMetadata> properties) {
        // Get all fields from the class and its superclasses
        Class<?> clazz = instance.getClass();
        while (clazz != null && !isSystemClass(clazz.getName())) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    // Skip static fields
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }

                    // Find the property metadata for this field
                    String fieldName = field.getName();
                    String propertyPath = findPropertyPathForField(properties, fieldName);
                    if (propertyPath == null) {
                        continue;
                    }

                    PropertyMetadata metadata = properties.get(propertyPath);
                    if (metadata == null) {
                        continue;
                    }

                    // Extract the field value
                    field.setAccessible(true);
                    Object value = field.get(instance);

                    // Convert to string representation
                    String defaultValue = convertToString(value);
                    if (defaultValue != null) {
                        metadata.setDefaultValue(defaultValue);
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract default value for field {}.{}: {}",
                              clazz.getSimpleName(), field.getName(), e.getMessage());
                }
            }

            // Process parent class
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Find the property path that corresponds to a field name.
     * The property path typically ends with the field name.
     *
     * @param properties map of property paths to PropertyMetadata objects
     * @param fieldName  the field name to search for
     * @return the matching property path, or null if not found
     */
    private static String findPropertyPathForField(Map<String, PropertyMetadata> properties, String fieldName) {
        for (String path : properties.keySet()) {
            if (path.endsWith("." + fieldName)) {
                return path;
            }
        }
        return null;
    }

    /**
     * Convert a field value to its string representation.
     *
     * @param value the field value
     * @return string representation of the value, or null if conversion fails
     */
    private static String convertToString(Object value) {
        if (value == null) {
            return null;
        }

        // Handle primitive types and strings
        if (value instanceof String ||
            value instanceof Number ||
            value instanceof Boolean ||
            value instanceof Character) {
            return value.toString();
        }

        // Handle special utility classes
        if (value instanceof HumanReadableNumber) {
            return value.toString();
        }
        if (value instanceof HumanReadableDuration) {
            return value.toString();
        }
        if (value instanceof HumanReadablePercentage) {
            return value.toString();
        }
        
        // Handle arrays
        if (value.getClass().isArray()) {
            try {
                ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
                int length = java.lang.reflect.Array.getLength(value);
                
                for (int i = 0; i < length; i++) {
                    Object item = java.lang.reflect.Array.get(value, i);
                    addValueToArrayNode(arrayNode, item);
                }
                return arrayNode.toString();
            } catch (Exception e) {
                log.debug("Failed to convert array to JSON: {}", e.getMessage());
                // Fall back to a simple representation
                return "[array of " + value.getClass().getComponentType().getSimpleName() + "]";
            }
        }

        // Handle collections
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                return "[]";
            }
            try {
                ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
                for (Object item : collection) {
                    addValueToArrayNode(arrayNode, item);
                }
                return arrayNode.toString();
            } catch (Exception e) {
                return "[" + collection.size() + " items]";
            }
        }

        // Handle maps
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                return "{}";
            }
            try {
                ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() != null) {
                        addValueToObjectNode(objectNode, entry.getKey().toString(), entry.getValue());
                    }
                }
                return objectNode.toString();
            } catch (Exception e) {
                return "{" + map.size() + " entries}";
            }
        }

        // For other types, use toString() as a fallback
        return value.toString();
    }

    /**
     * Add a value to a JSON array node.
     *
     * @param arrayNode the array node
     * @param value     the value to add
     */
    private static void addValueToArrayNode(ArrayNode arrayNode, Object value) {
        if (value == null) {
            arrayNode.addNull();
        } else if (value instanceof String) {
            arrayNode.add((String) value);
        } else if (value instanceof Integer) {
            arrayNode.add((Integer) value);
        } else if (value instanceof Long) {
            arrayNode.add((Long) value);
        } else if (value instanceof Float) {
            arrayNode.add((Float) value);
        } else if (value instanceof Double) {
            arrayNode.add((Double) value);
        } else if (value instanceof Boolean) {
            arrayNode.add((Boolean) value);
        } else {
            arrayNode.add(value.toString());
        }
    }

    /**
     * Add a value to a JSON object node.
     *
     * @param objectNode the object node
     * @param key        the key
     * @param value      the value to add
     */
    private static void addValueToObjectNode(ObjectNode objectNode, String key, Object value) {
        if (value == null) {
            objectNode.putNull(key);
        } else if (value instanceof String) {
            objectNode.put(key, (String) value);
        } else if (value instanceof Integer) {
            objectNode.put(key, (Integer) value);
        } else if (value instanceof Long) {
            objectNode.put(key, (Long) value);
        } else if (value instanceof Float) {
            objectNode.put(key, (Float) value);
        } else if (value instanceof Double) {
            objectNode.put(key, (Double) value);
        } else if (value instanceof Boolean) {
            objectNode.put(key, (Boolean) value);
        } else {
            objectNode.put(key, value.toString());
        }
    }

    /**
     * Check if a class is a system class that should not be processed.
     *
     * @param className the class name to check
     * @return true if it's a system class, false otherwise
     */
    private static boolean isSystemClass(String className) {
        // Skip JDK classes
        if (className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("sun.") ||
            className.startsWith("com.sun.") ||
            className.startsWith("jdk.")) {
            return true;
        }

        // Skip common library classes
        return className.startsWith("org.slf4j.") ||
               className.startsWith("ch.qos.logback.") ||
               className.startsWith("org.apache.commons.") ||
               className.startsWith("com.fasterxml.jackson.") ||
               className.startsWith("org.springframework.") ||
               className.startsWith("io.netty.");
    }
}
