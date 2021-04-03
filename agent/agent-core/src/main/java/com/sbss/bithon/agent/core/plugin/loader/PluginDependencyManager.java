package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.dependency.AgentDependencyManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mapping original classloader to AgentClassLoader
 *
 * @author frankchen
 * @date 2020-12-31 22:28:23
 */
public final class PluginDependencyManager {
    private static Map<ClassLoader, ClassLoader> classloaderMapping;

    /**
     * class loader for class which is being transformed.
     * it can be null if the class is loaded by bootstrap class loader
     */
    public static ClassLoader getClassLoader(ClassLoader appClassLoader) {
        return appClassLoader == null
               ? AgentDependencyManager.getClassLoader()
               : classloaderMapping.computeIfAbsent(appClassLoader, k -> new PluginClassLoader(appClassLoader));
    }

    public static void initialize() {
        classloaderMapping = new ConcurrentHashMap<>();

        ClassLoader appLoader = Thread.currentThread().getContextClassLoader();
        classloaderMapping.put(appLoader, new PluginClassLoader(appLoader));
    }
}
