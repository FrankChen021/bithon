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
import org.bithon.agent.instrumentation.utils.AgentDirectory;

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
    final Map<String, InterceptorType> interceptorTypes;
    final List<String> pluginClassList;

    public PluginMetadata(List<String> pluginClassList, Map<String, InterceptorType> interceptorTypes) {
        this.pluginClassList = pluginClassList;
        this.interceptorTypes = interceptorTypes;
    }

    static class Loader {

        public static PluginMetadata load(File file) {
            File pluginMetaFile = new File(AgentDirectory.getSubDirectory("plugins"), "plugins.meta");
            if (!pluginMetaFile.exists()) {
                throw new AgentException("Plugin metadata file not found. Please report it to agent maintainers.");
            }

            try (FileInputStream fileStream = new FileInputStream(pluginMetaFile)) {
                return load(fileStream);
            } catch (IOException e) {
                throw new AgentException("Unable to read plugin metadata file: %s", e.getMessage());
            }
        }

        /**
         * Parse INI-style properties file to extract plugin classes and interceptor types.
         * Format:
         * [plugin.class.name]
         * interceptor.class.name=INTERCEPTOR_TYPE
         *
         */
        private static PluginMetadata load(InputStream inputStream) throws IOException {
            Map<String, InterceptorType> interceptorTypes = new HashMap<>();
            List<String> pluginClassList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // Skip comments and empty lines
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    // Find plugin class declaration as [plugin.class.name]
                    if (line.startsWith("[") && line.endsWith("]")) {
                        String pluginClass = line.substring(1, line.length() - 1);
                        pluginClassList.add(pluginClass);
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

            return new PluginMetadata(pluginClassList, interceptorTypes);
        }
    }
}
