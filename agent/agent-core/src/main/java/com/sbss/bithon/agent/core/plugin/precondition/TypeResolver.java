package com.sbss.bithon.agent.core.plugin.precondition;

import shaded.net.bytebuddy.pool.TypePool;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class TypeResolver {

    private final static TypeResolver INSTANCE = new TypeResolver();
    public static TypeResolver getInstance() {
        return INSTANCE;
    }

    private final Map<ClassLoader, TypePool> pools = new HashMap<>();

    public boolean isResolved(ClassLoader classLoader, String clazz) {
        if ( classLoader == null ) {
            classLoader = BootstrapClassLoader.INSTANCE;
        }
        if (!pools.containsKey(classLoader)) {
            synchronized (pools) {
                // double check
                if (!pools.containsKey(classLoader)) {
                    TypePool pool = classLoader == BootstrapClassLoader.INSTANCE ? TypePool.Default.ofBootLoader() : TypePool.Default.of(classLoader);
                    pools.put(classLoader, pool);
                }
            }
        }
        TypePool pool = pools.get(classLoader);
        return pool.describe(clazz).isResolved();
    }

    static class BootstrapClassLoader extends ClassLoader {
        static BootstrapClassLoader INSTANCE = new BootstrapClassLoader();
    }
}
