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

package org.bithon.agent.core.aop.precondition;

import shaded.net.bytebuddy.pool.TypePool;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class TypeResolver {

    private static final TypeResolver INSTANCE = new TypeResolver();
    private final Map<ClassLoader, TypePool> pools = new HashMap<>();

    public static TypeResolver getInstance() {
        return INSTANCE;
    }

    public boolean isResolved(ClassLoader classLoader, String clazz) {
        if (classLoader == null) {
            classLoader = BootstrapClassLoader.INSTANCE;
        }
        if (!pools.containsKey(classLoader)) {
            synchronized (pools) {
                // double check
                if (!pools.containsKey(classLoader)) {
                    TypePool pool = classLoader == BootstrapClassLoader.INSTANCE
                                    ? TypePool.Default.ofBootLoader()
                                    : TypePool.Default.of(classLoader);
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
