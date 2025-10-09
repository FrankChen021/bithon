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

package org.bithon.agent.configuration.processor;

import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registry for loading and accessing configuration metadata at runtime.
 * Uses ClassLoader resource enumeration to discover metadata files from all modules.
 *
 * @author frank.chen021@outlook.com
 */
public class ConfigurationMetadataRegistry {

    private static final String METADATA_PATH = "META-INF/bithon-configuration-metadata.json";
    private static final ILogAdaptor log = LoggerFactory.getLogger(ConfigurationMetadataRegistry.class);

    private static volatile List<PropertyMetadata> cachedProperties;

    /**
     * Get all configuration properties from all modules.
     * Results are cached after first load.
     *
     * @return List of all property metadata
     */
    public static List<PropertyMetadata> getAllProperties() {
        if (cachedProperties == null) {
            synchronized (ConfigurationMetadataRegistry.class) {
                if (cachedProperties == null) {
                    cachedProperties = loadAllProperties();
                }
            }
        }
        return cachedProperties;
    }

    /**
     * Clear the cached properties. Useful for testing or when metadata might change.
     */
    public static void clearCache() {
        synchronized (ConfigurationMetadataRegistry.class) {
            cachedProperties = null;
        }
    }

    private static List<PropertyMetadata> loadAllProperties() {
        List<PropertyMetadata> allProperties = new ArrayList<>();

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = ConfigurationMetadataRegistry.class.getClassLoader();
            }

            Enumeration<URL> resources = classLoader.getResources(METADATA_PATH);
            int moduleCount = 0;

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                moduleCount++;
                
                log.info("Loading configuration metadata from: {}", resource);

                try (InputStream is = resource.openStream()) {
                    List<PropertyMetadata> properties = parseMetadata(is);
                    if (properties != null) {
                        allProperties.addAll(properties);
                    }
                } catch (Exception e) {
                    log.warn("Failed to load configuration metadata from {}: {}", resource, e.getMessage());
                }
            }

            log.info("Loaded {} configuration properties from {} modules", allProperties.size(), moduleCount);

        } catch (IOException e) {
            log.error("Failed to enumerate configuration metadata resources", e);
        }

        return Collections.unmodifiableList(allProperties);
    }

    private static List<PropertyMetadata> parseMetadata(InputStream is) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<List<PropertyMetadata>> typeRef = new TypeReference<List<PropertyMetadata>>() {};
            return mapper.readValue(is, typeRef);
        } catch (Exception e) {
            log.warn("Failed to parse configuration metadata", e);
            return null;
        }
    }

    /**
     * Get all properties that start with the given path prefix.
     *
     * @param pathPrefix The path prefix to filter by (e.g., "application", "tracing")
     * @return List of matching properties
     */
    public static List<PropertyMetadata> getPropertiesForPath(String pathPrefix) {
        return getAllProperties().stream()
            .filter(prop -> prop.getPath().startsWith(pathPrefix))
            .collect(Collectors.toList());
    }

    /**
     * Get metadata for a specific property by its full path.
     *
     * @param fullPath The full property path (e.g., "application.name")
     * @return Optional containing the property metadata if found
     */
    public static Optional<PropertyMetadata> getPropertyMetadata(String fullPath) {
        return getAllProperties().stream()
            .filter(prop -> prop.getPath().equals(fullPath))
            .findFirst();
    }

    /**
     * Get all properties that support dynamic configuration updates.
     *
     * @return List of dynamic properties
     */
    public static List<PropertyMetadata> getDynamicProperties() {
        return getAllProperties().stream()
            .filter(PropertyMetadata::isDynamic)
            .collect(Collectors.toList());
    }

    /**
     * Get all properties that are marked as required.
     *
     * @return List of required properties
     */
    public static List<PropertyMetadata> getRequiredProperties() {
        return getAllProperties().stream()
            .filter(PropertyMetadata::isRequired)
            .collect(Collectors.toList());
    }

    /**
     * Get all properties grouped by their root configuration path.
     *
     * @return List of root configuration paths
     */
    public static List<String> getConfigurationPaths() {
        return getAllProperties().stream()
            .map(prop -> prop.getPath().split("\\.")[0])
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Get properties by type.
     *
     * @param typeName The type name to filter by (e.g., "String", "boolean")
     * @return List of properties with the specified type
     */
    public static List<PropertyMetadata> getPropertiesByType(String typeName) {
        return getAllProperties().stream()
            .filter(prop -> prop.getType().equals(typeName) || prop.getType().endsWith("." + typeName))
            .collect(Collectors.toList());
    }

    /**
     * Get properties by configuration class.
     *
     * @param configurationClassName The fully qualified configuration class name
     * @return List of properties from the specified configuration class
     */
    public static List<PropertyMetadata> getPropertiesByConfigurationClass(String configurationClassName) {
        return getAllProperties().stream()
            .filter(prop -> configurationClassName.equals(prop.getConfigurationClass()))
            .collect(Collectors.toList());
    }

    /**
     * Get all unique configuration class names.
     *
     * @return List of configuration class names
     */
    public static List<String> getConfigurationClasses() {
        return getAllProperties().stream()
            .map(PropertyMetadata::getConfigurationClass)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
}
