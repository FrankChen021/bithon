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

package org.bithon.agent.core.plugin;

import org.bithon.agent.bootstrap.aop.BootstrapHelper;
import org.bithon.agent.bootstrap.loader.AgentClassLoader;
import org.bithon.agent.bootstrap.loader.JarClassLoader;
import org.bithon.agent.bootstrap.loader.JarResolver;
import org.bithon.agent.core.context.AgentContext;

import java.io.File;
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
    private static JarClassLoader defaultLoader;
    private static final Map<ClassLoader, ClassLoader> LOADER_MAPPING = new ConcurrentHashMap<>();

    /**
     * class loader for class which is being transformed.
     * it can be null if the class is loaded by bootstrap class loader
     */
    public static ClassLoader getClassLoader(ClassLoader appClassLoader) {
        return appClassLoader == null
               ? defaultLoader
               : LOADER_MAPPING.computeIfAbsent(appClassLoader,
                                                k -> new JarClassLoader("plugin",
                                                                        defaultLoader.getJars(),
                                                                        AgentClassLoader.getClassLoader(),
                                                                        appClassLoader));
    }

    public static JarClassLoader getDefaultLoader() {
        return defaultLoader;
    }

    public static void createDefault(String agentPath) {
        List<JarFile> pluginJars = JarResolver.resolve(new File(agentPath + "/" + AgentContext.PLUGIN_DIR));

        defaultLoader = new JarClassLoader("plugin", pluginJars, AgentClassLoader.getClassLoader());

        //
        // set the default plugin class loader to a class which could be access from classes loaded by bootstrap class loader
        //
        BootstrapHelper.setPluginClassLoader(defaultLoader);
    }
}
