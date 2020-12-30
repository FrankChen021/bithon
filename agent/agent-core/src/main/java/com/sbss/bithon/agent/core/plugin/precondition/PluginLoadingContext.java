package com.sbss.bithon.agent.core.plugin.precondition;

import shaded.net.bytebuddy.pool.TypePool;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 1:13 下午
 */
public class PluginLoadingContext {
    private final TypePool typePool;
    private final ClassLoader classLoader;

    public PluginLoadingContext(ClassLoader classLoader, TypePool typePool) {
        this.typePool = typePool;
        this.classLoader = classLoader;
    }

    public TypePool getTypePool() {
        return typePool;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }
}
