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

import org.bithon.agent.instrumentation.loader.PluginClassLoader;
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

    private static List<PropertyMetadata> loadAllProperties() {
        List<PropertyMetadata> allProperties = new ArrayList<>();

        try {
            Enumeration<URL> resources = PluginClassLoader.getClassLoader().getResources(METADATA_PATH);
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
            TypeReference<List<PropertyMetadata>> typeRef = new TypeReference<List<PropertyMetadata>>() {
            };
            return mapper.readValue(is, typeRef);
        } catch (Exception e) {
            log.warn("Failed to parse configuration metadata", e);
            return null;
        }
    }
}
