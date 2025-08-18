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

package org.bithon.agent.controller.configuration;

import org.bithon.agent.configuration.metadata.DefaultValueExtractor;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.agent.instrumentation.loader.AgentClassLoader;
import org.bithon.agent.instrumentation.loader.JarClassLoader;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loading and accessing configuration metadata at runtime.
 * Uses ClassLoader resource enumeration to discover metadata files from all modules.
 *
 * @author frank.chen021@outlook.com
 */
public class ConfigurationPropertyLoader {

    private static final ILogAdaptor log = LoggerFactory.getLogger(ConfigurationPropertyLoader.class);

    private static volatile List<PropertyMetadata> cachedProperties;

    /**
     * Get all configuration properties from all modules.
     * Results are cached after first load.
     *
     * @return List of all property metadata
     */
    public static List<PropertyMetadata> loadAllProperties() {
        if (cachedProperties == null) {
            synchronized (ConfigurationPropertyLoader.class) {
                if (cachedProperties == null) {
                    cachedProperties = loadImpl();
                }
            }
        }
        return cachedProperties;
    }

    private static List<PropertyMetadata> loadImpl() {
        List<PropertyMetadata> allProperties = new ArrayList<>();

        try {
            // Iterate through both PluginClassLoader and AgentClassLoader JARs
            JarClassLoader[] classLoaders = {
                (JarClassLoader) PluginClassLoader.getClassLoader(),
                AgentClassLoader.getClassLoader()
            };

            List<URL> discoveredMetadataPaths = new ArrayList<>();
            for (JarClassLoader classLoader : classLoaders) {
                if (classLoader != null) {
                    List<JarFile> jars = classLoader.getJars();
                    log.info("Scanning {} JARs in {}", jars.size(), classLoader.toString());

                    for (JarFile jarFile : jars) {
                        try {
                            discoveredMetadataPaths.addAll(scanJarForConfigurationMetadata(classLoader, jarFile));
                        } catch (Exception e) {
                            log.warn("Failed to scan JAR {} for configuration metadata: {}",
                                     jarFile.getName(), e.getMessage());
                        }
                    }
                }
            }

            // Load configuration metadata for discovered paths
            for (URL metadataPath : discoveredMetadataPaths) {
                try {
                    try (InputStream is = metadataPath.openStream()) {
                        List<PropertyMetadata> properties = parseMetadata(is);
                        if (properties != null) {
                            allProperties.addAll(properties);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load configuration metadata from {}: {}", metadataPath, e.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("Failed to load configuration metadata from path {}: {}", metadataPath, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to enumerate configuration metadata resources", e);
        }

        // Sort for better readability
        allProperties.sort(Comparator.comparing(PropertyMetadata::getPath));

        // Extract default values from configuration class instances
        new DefaultValueExtractor((className -> {
            try {
                return Class.forName(className, true, PluginClassLoader.getClassLoader());
            } catch (ClassNotFoundException e) {
                return Class.forName(className, true, AgentClassLoader.getClassLoader());
            }
        })).extract(allProperties);

        return Collections.unmodifiableList(allProperties);
    }

    private static List<URL> scanJarForConfigurationMetadata(ClassLoader classLoader, JarFile jarFile) {
        List<URL> discoveredMetadataPaths = new ArrayList<>();
        {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String entryName = jarEntry.getName();

                // Look for configuration metadata files
                if (entryName.startsWith("META-INF/bithon/configuration/") &&
                    entryName.endsWith(".meta")) {
                    discoveredMetadataPaths.add(classLoader.getResource(entryName));
                }
            }
        }
        return discoveredMetadataPaths;
    }

    private static List<PropertyMetadata> parseMetadata(InputStream is) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<List<PropertyMetadata>> typeRef = new TypeReference<List<PropertyMetadata>>() {
            };
            return mapper.readValue(is, typeRef);
        } catch (Exception e) {
            log.warn("Failed to parse configuration metadata", e);
            return null;
        }
    }
}
