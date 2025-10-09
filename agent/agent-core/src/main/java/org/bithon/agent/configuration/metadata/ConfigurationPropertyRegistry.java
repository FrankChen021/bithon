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

package org.bithon.agent.configuration.metadata;

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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Registry for loading and accessing configuration metadata at runtime.
 * Uses ClassLoader resource enumeration to discover metadata files from all modules.
 *
 * @author frank.chen021@outlook.com
 */
public class ConfigurationPropertyRegistry {

    private static final ILogAdaptor log = LoggerFactory.getLogger(ConfigurationPropertyRegistry.class);

    private static volatile List<PropertyMetadata> cachedProperties;

    /**
     * Get all configuration properties from all modules.
     * Results are cached after first load.
     *
     * @return List of all property metadata
     */
    public static List<PropertyMetadata> getAllProperties() {
        if (cachedProperties == null) {
            synchronized (ConfigurationPropertyRegistry.class) {
                if (cachedProperties == null) {
                    cachedProperties = loadAllProperties();
                }
            }
        }
        return cachedProperties;
    }

    private static List<PropertyMetadata> loadAllProperties() {
        List<PropertyMetadata> allProperties = new ArrayList<>();
        Set<String> discoveredMetadataPaths = new HashSet<>();
        int moduleCount = 0;

        try {
            // Iterate through both PluginClassLoader and AgentClassLoader JARs
            JarClassLoader[] classLoaders = {
                (JarClassLoader) PluginClassLoader.getClassLoader(),
                AgentClassLoader.getClassLoader()
            };

            for (JarClassLoader classLoader : classLoaders) {
                if (classLoader != null) {
                    List<JarFile> jars = classLoader.getJars();
                    log.info("Scanning {} JARs in {}", jars.size(), classLoader.toString());

                    for (JarFile jarFile : jars) {
                        try {
                            Set<String> metadataPathsFromJar = scanJarForConfigurationMetadata(jarFile);
                            discoveredMetadataPaths.addAll(metadataPathsFromJar);
                        } catch (Exception e) {
                            log.warn("Failed to scan JAR {} for configuration metadata: {}", 
                                   jarFile.getName(), e.getMessage());
                        }
                    }
                }
            }

            // Load configuration metadata for discovered paths
            for (String metadataPath : discoveredMetadataPaths) {
                try {
                    // Try to load from PluginClassLoader first, then AgentClassLoader
                    URL resource = PluginClassLoader.getClassLoader().getResource(metadataPath);
                    if (resource == null) {
                        resource = AgentClassLoader.getClassLoader().getResource(metadataPath);
                    }
                    
                    if (resource != null) {
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
                } catch (Exception e) {
                    log.warn("Failed to load configuration metadata from path {}: {}", metadataPath, e.getMessage());
                }
            }

            log.info("Loaded {} configuration properties from {} modules", allProperties.size(), moduleCount);

        } catch (Exception e) {
            log.error("Failed to enumerate configuration metadata resources", e);
        }

        return Collections.unmodifiableList(allProperties);
    }

    private static Set<String> scanJarForConfigurationMetadata(JarFile jarFile) {
        Set<String> discoveredMetadataPaths = new HashSet<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            
            // Look for configuration metadata files
            if (entryName.startsWith("META-INF/bithon/configuration/") && 
                entryName.endsWith(".meta")) {
                
                // Add the full path directly
                discoveredMetadataPaths.add(entryName);
                
                // Extract module name for logging
                String fileName = entryName.substring("META-INF/bithon/configuration/".length());
                if (fileName.endsWith(".meta")) {
                    String moduleName = fileName.substring(0, fileName.length() - ".meta".length());
                    log.debug("Discovered module configuration metadata: {} at {}", moduleName, entryName);
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
