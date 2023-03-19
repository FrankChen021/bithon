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

package org.bithon.agent.bootstrap.loader;

import org.bithon.agent.bootstrap.utils.AgentDirectory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                                                k -> new CascadingClassLoader(defaultLoader,
                                                                              appClassLoader));
    }

    public static JarClassLoader getDefaultLoader() {
        return defaultLoader;
    }

    public static synchronized void createDefault() {
        if (defaultLoader == null) {
            defaultLoader = new JarClassLoader("plugin",
                                               AgentDirectory.getSubDirectory("plugins"),
                                               AgentClassLoader.getClassLoader());
        }
    }
}
