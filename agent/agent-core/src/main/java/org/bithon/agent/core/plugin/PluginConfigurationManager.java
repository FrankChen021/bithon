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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/11 16:31
 */
public class PluginConfigurationManager {

    /**
     * load plugin configuration from static plugin.yml and dynamic configuration from environment variable and command line arguments
     */
    public static Configuration load(Class<? extends IPlugin> pluginClass) {
        String name = pluginClass.getPackage().getName() + ".yml";
        String dynamicPrefix = "bithon." + getPluginConfigurationPrefixName(pluginClass.getName());

        try (InputStream is = pluginClass.getClassLoader().getResourceAsStream(name)) {
            return Configuration.create(name, is, dynamicPrefix);
        } catch (IOException ignored) {
            // ignore this exception thrown from InputStream.close
            // try to load from dynamic configuration
            return Configuration.create(name, null, dynamicPrefix);
        }
    }

    public static String getPluginConfigurationPrefixName(String pluginClassName) {
        String prefix = "org.bithon.agent.plugin.";
        if (!pluginClassName.startsWith(prefix)) {
            throw new AgentException("Plugin class name[%s] does not under 'org.bithon.agent.plugin' package.");
        }

        String[] parts = pluginClassName.substring(prefix.length()).split("\\.");
        if (parts.length <= 1) {
            throw new AgentException("Package name of [%s] does not contain a plugin name.");
        }

        return "agent.plugin." + Arrays.stream(parts,
                                               0,
                                               parts.length - 1/*exclusive*/)
                                       .collect(Collectors.joining("."))
               + ".";
    }
}
