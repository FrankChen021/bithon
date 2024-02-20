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

import org.bithon.agent.configuration.source.CommandLineArgsSource;
import org.bithon.agent.configuration.source.EnvironmentSource;
import org.bithon.agent.configuration.source.PropertySource;
import org.bithon.agent.configuration.source.PropertySourceType;
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
    public static boolean load(Class<?> pluginClass) {
        String pluginPropertyPrefix = getConfigurationPrefix(pluginClass.getName());

        PropertySource pluginPropertySource = load(pluginClass, pluginPropertyPrefix);
        if (!pluginPropertySource.isEmpty()) {
            ConfigurationManager.getInstance().addPropertySource(pluginPropertySource);
        }

        // Even there's no plugin configuration found above, it can be in the external configuration
        // So, check the 'disable' property from the manager to see if it's DISABLED
        Boolean isPluginDisabled = ConfigurationManager.getInstance().getConfig(pluginPropertyPrefix + ".disabled", Boolean.class);
        if (isPluginDisabled != null && isPluginDisabled) {
            return false;
        }

        return true;
    }

    /**
     * Load plugin configuration from static plugin.yml and dynamic configuration from environment variable and command line arguments
     */
    private static PropertySource load(Class<?> pluginClass, String propertyPathPrefix) {
        String configFileName = pluginClass.getPackage().getName() + ".yml";

        PropertySource defaultPluginPropertySource;
        try (InputStream fs = pluginClass.getClassLoader().getResourceAsStream(configFileName)) {
            defaultPluginPropertySource = PropertySource.from(PropertySourceType.INTERNAL, configFileName, ConfigurationFormat.YAML, fs);
        } catch (IOException ignored) {
            // Ignore this exception thrown from InputStream.close
            // Create an empty one for further processing
            defaultPluginPropertySource = new PropertySource(PropertySourceType.INTERNAL, configFileName);
        }

        // Use the command line args and environment to overwrite the default properties
        String dynamicPropertyPrefix = "bithon." + propertyPathPrefix + ".";
        return defaultPluginPropertySource.merge(CommandLineArgsSource.build(dynamicPropertyPrefix))
                                          .merge(EnvironmentSource.build(dynamicPropertyPrefix));
    }

    /**
     * Get the configuration prefix for a given plugin.
     * The package name for a plugin SHOULD be: org.bithon.agent.plugin.xxx.
     * The prefix should be: bithon.agent.plugin.xxx.
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
