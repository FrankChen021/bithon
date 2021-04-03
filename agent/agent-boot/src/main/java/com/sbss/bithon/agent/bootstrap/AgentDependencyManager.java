package com.sbss.bithon.agent.bootstrap;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/31 21:44
 */
public class AgentDependencyManager {

    private static JarClassLoader instance;

    public static ClassLoader getClassLoader() {
        return instance;
    }

    /**
     * initialize class loader as a cascaded class loader
     */
    public static ClassLoader initialize(File agentDirectory) {
        instance = new JarClassLoader(new File(agentDirectory, "plugins"),
                                      new JarClassLoader(new File(agentDirectory, "lib"),
                                                         Thread.currentThread().getContextClassLoader()));
        return instance;
    }

    public static List<JarFile> getPlugins() {
        return Collections.unmodifiableList(instance.jars);
    }
}
