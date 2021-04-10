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

import com.sbss.bithon.agent.boot.loader.AgentDependencyManager;

import java.net.URL;

/**
 * Class loader for agent to load interceptors and their application class dependencies
 * <p>
 * This class loader has two parents:
 * 1. AgentClassLoader, which is responsible for loading classes from directories 'lib' and 'plugins'
 * 2. Application class loader, which is responsible for loading application classes. Since interceptors rely on application classes
 * this ensures that those classes could be found for interceptors
 *
 * @author frankchen
 * @date 2020-12-31 22:25:45
 */
class PluginClassLoader extends ClassLoader {

    private final ClassLoader[] parents;

    PluginClassLoader(ClassLoader appClassLoader) {
        this.parents = new ClassLoader[]{AgentDependencyManager.getClassLoader(), appClassLoader};
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader parent : parents) {
            try {
                return parent.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name);
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader parent : parents) {
            URL url = parent.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }
}
