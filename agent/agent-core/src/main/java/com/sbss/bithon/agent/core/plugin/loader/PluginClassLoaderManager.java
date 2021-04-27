/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.bootstrap.loader.AgentDependencyManager;
import com.sbss.bithon.agent.bootstrap.loader.JarClassLoader;
import com.sbss.bithon.agent.bootstrap.loader.JarResolver;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * mapping original classloader to AgentClassLoader
 *
 * @author frankchen
 * @date 2020-12-31 22:28:23
 */
public final class PluginClassLoaderManager {
    private static ClassLoader defaultLoader;
    private static final Map<ClassLoader, ClassLoader> LOADER_MAPPING = new ConcurrentHashMap<>();
    private static List<JarFile> pluginJars;

    /**
     * class loader for class which is being transformed.
     * it can be null if the class is loaded by bootstrap class loader
     */
    public static ClassLoader getClassLoader(ClassLoader appClassLoader) {
        return appClassLoader == null
               ? defaultLoader
               : LOADER_MAPPING.computeIfAbsent(appClassLoader,
                                                k -> new JarClassLoader("plugin",
                                                                        pluginJars,
                                                                        AgentDependencyManager.getClassLoader(),
                                                                        appClassLoader));
    }

    public static ClassLoader createDefault(String agentPath) {
        pluginJars = JarResolver.resolve(new File(agentPath + "/" + AgentContext.PLUGIN_DIR));

        defaultLoader = new JarClassLoader("plugin", pluginJars, AgentDependencyManager.getClassLoader());
        return defaultLoader;
    }

    public static List<AbstractPlugin> resolvePlugins() {
        final List<AbstractPlugin> plugins = new ArrayList<>();
        for (JarFile jar : pluginJars) {
            try {
                String pluginClassName = jar.getManifest().getMainAttributes().getValue("Plugin-Class");
                AbstractPlugin plugin = (AbstractPlugin) Class.forName(pluginClassName,
                                                                       true,
                                                                       defaultLoader)
                                                              .newInstance();
                plugins.add(plugin);
            } catch (Throwable e) {
                LoggerFactory.getLogger(PluginClassLoaderManager.class)
                             .error(String.format("Failed to add plugin from jar %s",
                                                  new File(jar.getName()).getName()),
                                    e);
            }
        }
        return plugins;
    }
}
