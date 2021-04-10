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
