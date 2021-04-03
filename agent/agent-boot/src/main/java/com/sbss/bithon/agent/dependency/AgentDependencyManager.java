package com.sbss.bithon.agent.dependency;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/31 21:44
 */
public class AgentDependencyManager {

    private static JarsClassLoader instance;

    public static ClassLoader getClassLoader() {
        return instance;
    }

    /**
     * initialize class loader as a cascaded class loader
     */
    public static ClassLoader initialize(File agentDirectory) {
        instance = new JarsClassLoader(new File(agentDirectory, "plugins"),
                                       new JarsClassLoader(new File(agentDirectory, "lib"),
                                                           Thread.currentThread().getContextClassLoader()));
        return instance;
    }

    public static List<JarFileItem> getPlugins() {
        return Collections.unmodifiableList(instance.jars);
    }
}
