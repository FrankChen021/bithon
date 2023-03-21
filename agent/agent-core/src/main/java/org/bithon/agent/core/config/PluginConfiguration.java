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

package org.bithon.agent.core.config;

import org.bithon.agent.instrumentation.expt.AgentException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:31
 */
public class PluginConfiguration {

    /**
     * @return false is the plugin is disabled by configuration
     */
    public static boolean load(Class<?> pluginClass) {
        String pluginConfigurationPrefix = getConfigurationPrefix(pluginClass.getName());

        Configuration pluginConfiguration = load(pluginClass, pluginConfigurationPrefix);
        if (!pluginConfiguration.isEmpty()) {

            Boolean isPluginDisabled = pluginConfiguration.getConfig(pluginConfigurationPrefix + ".disabled", Boolean.class);
            if (isPluginDisabled != null && isPluginDisabled) {
                return false;
            }
        }

        // Merge the plugin configuration into agent configuration first so that the plugin initialization can obtain its configuration
        ConfigurationManager.getInstance().merge(pluginConfiguration);

        return true;
    }

    /**
     * Load plugin configuration from static plugin.yml and dynamic configuration from environment variable and command line arguments
     */
    private static Configuration load(Class<?> pluginClass, String configurationPrefix) {
        String configFormat = pluginClass.getPackage().getName() + ".yml";
        String dynamicPrefix = "bithon." + configurationPrefix + ".";

        Configuration pluginConfiguration;
        try (InputStream staticConfigurationStream = pluginClass.getClassLoader().getResourceAsStream(configFormat)) {
            pluginConfiguration = Configuration.create(configFormat, staticConfigurationStream, dynamicPrefix);
        } catch (IOException ignored) {
            // Ignore this exception thrown from InputStream.close
            // Try to load from dynamic configuration
            pluginConfiguration = Configuration.create(configFormat, null, dynamicPrefix);
        }

        if (!pluginConfiguration.isEmpty() && !pluginConfiguration.validate(configurationPrefix)) {
            // Dump file content if it's invalid
            StringBuilder config = new StringBuilder(128);
            try {
                try (InputStream stream = pluginClass.getClassLoader().getResourceAsStream(configFormat)) {
                    if (stream != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                config.append(line);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            }
            throw new AgentException("Plugin [%s] has a configuration that does not comply with the configuration prefix [%s]:\n%s",
                                     pluginClass.getName(),
                                     configurationPrefix,
                                     config.toString()
            );
        }
        return pluginConfiguration;
    }

    /**
     * Get the configuration prefix for given plugin
     */
    private static String getConfigurationPrefix(String pluginClassName) {
        String prefix = "org.bithon.agent.plugin.";
        if (!pluginClassName.startsWith(prefix)) {
            throw new AgentException("Plugin class name[%s] does not under 'org.bithon.agent.plugin.' package.");
        }

        String[] parts = pluginClassName.substring(prefix.length()).split("\\.");
        if (parts.length <= 1) {
            throw new AgentException("Package name of [%s] does not contain a plugin name.");
        }

        return "agent.plugin." + Arrays.stream(parts,
                                               0,
                                               parts.length - 1)
                                       .collect(Collectors.joining("."));
    }
}
