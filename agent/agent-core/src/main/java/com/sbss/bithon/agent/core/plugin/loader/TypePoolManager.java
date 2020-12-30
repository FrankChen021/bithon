package com.sbss.bithon.agent.core.plugin.loader;

import shaded.net.bytebuddy.pool.TypePool;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 1:22 下午
 */
public class TypePoolManager {
    private static final Map<ClassLoader, TypePool> pools = new HashMap<>();

    public static TypePool getOrCreateTypePool(ClassLoader classLoader) {
        if (!pools.containsKey(classLoader)) {
            synchronized (pools) {
                if (!pools.containsKey(classLoader)) { // double check
                    pools.put(classLoader, TypePool.Default.of(classLoader));
                }
            }
        }
        return pools.get(classLoader);
    }
}
