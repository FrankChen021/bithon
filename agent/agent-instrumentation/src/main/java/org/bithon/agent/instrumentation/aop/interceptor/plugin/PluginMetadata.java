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

package org.bithon.agent.instrumentation.aop.interceptor.plugin;


import org.bithon.agent.instrumentation.aop.interceptor.InterceptorType;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 14/8/25 9:13 pm
 */
class PluginMetadata {
    /**
     * All interceptors
     * key: interceptor class name
     */
    final Map<String, InterceptorType> interceptorTypes;
    final List<PluginInfo> pluginInfoList;

    public PluginMetadata(List<PluginInfo> pluginInfoList,
                          Map<String, InterceptorType> interceptorTypes) {
        this.pluginInfoList = pluginInfoList;
        this.interceptorTypes = interceptorTypes;
    }

    static class PluginInfo {
        final String className;
        final int minimalJdkVersion;

        public PluginInfo(String className, int minimalJdkVersion) {
            this.className = className;
            this.minimalJdkVersion = minimalJdkVersion;
        }
    }

    static class Loader {

        public static PluginMetadata load(File file) {
            if (!file.exists()) {
                throw new AgentException("Plugin metadata file not found. Please report it to agent maintainers.");
            }

            try (FileInputStream fileStream = new FileInputStream(file)) {
                return parseFromInputStream(fileStream);
            } catch (IOException e) {
                throw new AgentException("Unable to read plugin metadata file: %s", e.getMessage());
            }
        }

        /**
         * Parse INI-style properties file to extract plugin classes and interceptor types.
         * Format:
         * [plugin.class.name, property1=value1, property2=value2]
         * interceptor.class.name=INTERCEPTOR_TYPE
         *
         */
        static PluginMetadata parseFromInputStream(InputStream inputStream) throws IOException {
            Map<String, InterceptorType> interceptorTypes = new HashMap<>();
            List<PluginInfo> pluginInfoList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip comments and empty lines
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Find plugin class declaration as [plugin.class.name] or [plugin.class.name, properties...]
                    if (line.startsWith("[") && line.endsWith("]")) {
                        String content = line.substring(1, line.length() - 1);
                        PluginInfo pluginInfo = parsePluginDeclaration(content);
                        pluginInfoList.add(pluginInfo);
                        continue;
                    }

                    int equalIndex = line.indexOf('=');
                    if (equalIndex > 0) {
                        String interceptorClassName = line.substring(0, equalIndex).trim();
                        String typeString = line.substring(equalIndex + 1).trim();

                        try {
                            interceptorTypes.put(interceptorClassName, InterceptorType.valueOf(typeString));
                        } catch (IllegalArgumentException e) {
                            LoggerFactory.getLogger(PluginMetadata.class)
                                         .warn("Invalid interceptor type [{}] for interceptor [{}]", typeString, interceptorClassName);
                        }
                    }
                }
            }

            return new PluginMetadata(pluginInfoList, interceptorTypes);
        }

        /**
         * Parse a plugin declaration line and extract plugin class name and properties.
         * Format: plugin.class.name, property1=value1, property2=value2
         *
         * @param content The content inside the brackets without the brackets themselves
         * @return PluginInfo instance with parsed information
         */
        private static PluginInfo parsePluginDeclaration(String content) {
            // Split by comma to separate plugin class name from properties
            String[] parts = content.split(",");
            String pluginClass = parts[0].trim();

            //
            // Process properties if any
            //
            int minimalJdkVersion = 0; // Default to 8 if not specified
            for (int i = 1; i < parts.length; i++) {
                String property = parts[i].trim();
                int equalIndex = property.indexOf('=');
                if (equalIndex > 0) {
                    String propertyName = property.substring(0, equalIndex).trim();
                    String propertyValue = property.substring(equalIndex + 1).trim();

                    if ("minimalJdkVersion".equals(propertyName)) {
                        try {
                            minimalJdkVersion = Integer.parseInt(propertyValue);
                        } catch (NumberFormatException e) {
                            LoggerFactory.getLogger(PluginMetadata.class)
                                         .warn("Invalid minimalJdkVersion [{}] for plugin [{}]", propertyValue, pluginClass);
                        }
                    }
                    // Future properties can be added here
                }
            }
            if (minimalJdkVersion == 0) {
                throw new AgentException("Unknown minimalJdkVersion for plugin [%s]. Please report it to agent maintainers.", pluginClass);
            }

            return new PluginInfo(pluginClass, minimalJdkVersion);
        }
    }
}
