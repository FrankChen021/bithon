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

package org.bithon.agent.core.plugin;

import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.core.config.Configuration;

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
class PluginConfiguration {

    /**
     * Load plugin configuration from static plugin.yml and dynamic configuration from environment variable and command line arguments
     */
    static Configuration load(Class<?> pluginClass) {
        String configFormat = pluginClass.getPackage().getName() + ".yml";
        String pluginConfigurationPrefix = getPluginConfigurationPrefixName(pluginClass.getName());
        String dynamicPrefix = "bithon." + pluginConfigurationPrefix + ".";

        Configuration pluginConfiguration;
        try (InputStream staticConfigurationStream = pluginClass.getClassLoader().getResourceAsStream(configFormat)) {
            pluginConfiguration = Configuration.create(configFormat, staticConfigurationStream, dynamicPrefix);
        } catch (IOException ignored) {
            // Ignore this exception thrown from InputStream.close
            // Try to load from dynamic configuration
            pluginConfiguration = Configuration.create(configFormat, null, dynamicPrefix);
        }

        if (!pluginConfiguration.isEmpty() && !pluginConfiguration.validate(pluginConfigurationPrefix)) {
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
                                     pluginConfigurationPrefix,
                                     config.toString()
            );
        }
        return pluginConfiguration;
    }

    static String getPluginConfigurationPrefixName(String pluginClassName) {
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
