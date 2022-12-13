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

import org.bithon.agent.bootstrap.loader.JarClassLoader;
import org.bithon.agent.bootstrap.loader.PluginClassLoaderManager;
import org.bithon.agent.core.aop.descriptor.Descriptors;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-20:36
 */
public class PluginManager {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(PluginManager.class);

    private final List<IPlugin> plugins;

    public PluginManager(AgentContext agentContext) {
        // create plugin class loader first
        PluginClassLoaderManager.createDefault(agentContext.getAgentDirectory());

        // load plugins
        plugins = loadPlugins();
    }

    public Descriptors getInterceptorDescriptors() {
        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            String pluginName = plugin.getClass().getSimpleName();

            descriptors.merge(plugin.getBithonClassDescriptor());

            descriptors.merge(pluginName, plugin.getPreconditions(), plugin.getInterceptors());
        }
        return descriptors;
    }

    public void start() {
        plugins.forEach(IPlugin::start);
    }

    public void stop() {
        plugins.forEach(IPlugin::stop);
    }

    private List<IPlugin> loadPlugins() {
        JarClassLoader pluginClassLoader = PluginClassLoaderManager.getDefaultLoader();
        return pluginClassLoader.getJars()
                                .stream()
                                .flatMap(JarFile::stream)
                                .filter(jarEntry -> jarEntry.getName().endsWith("Plugin.class"))
                                .sorted(Comparator.comparing(JarEntry::getName))
                                .map((jarEntry) -> loadPlugin(jarEntry, pluginClassLoader))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
    }

    private IPlugin loadPlugin(JarEntry jarEntry, JarClassLoader pluginClassLoader) {
        String jarEntryName = jarEntry.getName();
        String pluginClassName = jarEntryName.substring(0, jarEntryName.length() - ".class".length()).replace('/', '.');

        try {
            Class<?> pluginClass = Class.forName(pluginClassName, true, pluginClassLoader);

            Boolean isPluginDisabled = PluginConfigurationManager.load(pluginClass)
                                                                 .getConfig(PluginConfigurationManager.getPluginConfigurationPrefixName(pluginClassName) + "disabled",
                                                                            Boolean.class);
            if (isPluginDisabled != null && isPluginDisabled) {
                LOG.info("Found plugin {}, but it's DISABLED by configuration", pluginClassName);
                return null;
            }

            LOG.info("Found plugin {}", pluginClassName);
            Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
            if (pluginInstance instanceof IPlugin) {
                return (IPlugin) pluginInstance;
            } else {
                LOG.info("Resource [{}] is not type of IPlugin. The class name does not comply with the plugin standard. Please change it.", pluginClassName);
                return null;
            }
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Failed to load plugin [%s]", pluginClassName), e);
            return null;
        }
    }
}
