package com.sbss.bithon.agent.core.plugin.loader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * mapping original classloader to AgentClassLoader
 *
 * @author frankchen
 * @date 2020-12-31 22:28:23
 */
public final class AgentClassloaderManager {
    private static final Map<ClassLoader, ClassLoader> classloaderMapping = new ConcurrentHashMap<>();

    /**
     * class loader for class which is being transformed.
     * it can be null if the class is loaded by bootstrap class loader
     */
    public static ClassLoader getAgentLoader(ClassLoader classloader) {
        return classloader == null ?
            AgentClassloader.getDefaultInstance() :
            classloaderMapping.computeIfAbsent(classloader, k -> new AgentClassloader(classloader));
    }

    public static void register(ClassLoader originClassloader,
                                ClassLoader agentClassloader) {
        classloaderMapping.putIfAbsent(originClassloader, agentClassloader);
    }
}
