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

package org.bithon.agent.configuration;

import org.bithon.agent.instrumentation.expt.AgentException;

import java.io.IOException;
import java.io.InputStream;
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
    public static boolean loadPluginConfiguration(Class<?> pluginClass) {
        String pluginConfigurationPrefix = getConfigurationPrefix(pluginClass.getName());

        Configuration pluginConfiguration = loadPluginConfiguration(pluginClass, pluginConfigurationPrefix);
        if (pluginConfiguration.contains(pluginConfigurationPrefix)) {
            // Merge the plugin configuration into agent configuration first so that the plugin initialization can obtain its configuration
            ConfigurationManager.getInstance().merge(pluginConfiguration);
        }

        // Since the plugin configuration has been merged into the ConfigurationManager,
        // check the 'disable' property from the manager to see if it's DISABLED
        Boolean isPluginDisabled = ConfigurationManager.getInstance().getConfig(pluginConfigurationPrefix + ".disabled", Boolean.class);
        if (isPluginDisabled != null && isPluginDisabled) {
            return false;
        }

        return true;
    }

    /**
     * Load plugin configuration from static plugin.yml and dynamic configuration from environment variable and command line arguments
     */
    private static Configuration loadPluginConfiguration(Class<?> pluginClass, String configurationPrefix) {
        String configFileLocation = pluginClass.getPackage().getName() + ".yml";
        String dynamicPropertyPrefix = "bithon." + configurationPrefix + ".";

        Configuration defaultPluginConfiguration;
        try (InputStream fs = pluginClass.getClassLoader().getResourceAsStream(configFileLocation)) {
            defaultPluginConfiguration = Configuration.from(ConfigurationFormat.YAML, fs);
        } catch (IOException ignored) {
            // Ignore this exception thrown from InputStream.close
            // Try to load from dynamic configuration
            defaultPluginConfiguration = new Configuration();
        }

        // No need to load external configuration
        // because the plugin configuration will finally merge into the main configuration
        return defaultPluginConfiguration.merge(Configuration.fromCommandLineArgs(dynamicPropertyPrefix))
                                         .merge(Configuration.fromEnvironmentVariables(dynamicPropertyPrefix));
    }

    /**
     * Get the configuration prefix for a given plugin
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
