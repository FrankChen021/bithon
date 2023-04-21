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

package org.bithon.agent.instrumentation.aop.interceptor;


import org.bithon.agent.instrumentation.aop.interceptor.declaration.AbstractInterceptor;
import org.bithon.agent.instrumentation.loader.PluginClassLoaderManager;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor is singleton
 *
 * @author frankchen
 */
public class InterceptorManager {

    private static final ILogger LOG = LoggerFactory.getLogger(InterceptorManager.class);

    private int interceptorIndex = 0;

    public static final InterceptorManager INSTANCE = new InterceptorManager();

    private InterceptorSupplier[] interceptorList = new InterceptorSupplier[64];

    /**
     * key - interceptor class name
     * val - interceptor instance based on target class's class loader
     * key - System.identityHashCode of class loader
     * val - interceptor instance
     */
    private final Map<String, Map<String, InterceptorSupplier>> interceptorMaps = new ConcurrentHashMap<>();

    /**
     * Get interceptor by given index.
     */
    public InterceptorSupplier getSupplier(int index) {
        return index < interceptorList.length ? interceptorList[index] : null;
    }

    public static class InterceptorSupplier {
        private String interceptorClassName;
        private ClassLoader classLoader;

        private volatile AbstractInterceptor interceptor;
        private final int index;
        private boolean initialized = false;

        public InterceptorSupplier(int index,
                                   String interceptorClassName,
                                   ClassLoader classLoader) {
            this.index = index;
            this.interceptorClassName = interceptorClassName;
            this.classLoader = classLoader;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public AbstractInterceptor get() {
            if (interceptor != null) {
                return interceptor;
            }

            synchronized (this) {
                // Double check
                if (interceptor != null) {
                    return interceptor;
                }

                interceptor = createInterceptor(interceptorClassName, classLoader);
                if (interceptor != null) {
                    // Release reference so that GC works
                    this.classLoader = null;
                    this.interceptorClassName = null;
                }
                initialized = true;
                return interceptor;
            }
        }

        private AbstractInterceptor createInterceptor(String interceptorClassName, ClassLoader userClassLoader) {
            try {
                // Load class out of lock in case of deadlock
                ClassLoader interceptorClassLoader = PluginClassLoaderManager.getClassLoader(userClassLoader);
                Class<?> interceptorClass = Class.forName(interceptorClassName, true, interceptorClassLoader);

                return (AbstractInterceptor) interceptorClass.getConstructor().newInstance();
            } catch (Throwable e) {
                LOG.error(String.format(Locale.ENGLISH,
                                        "Failed to load interceptor[%s] due to %s",
                                        interceptorClassName,
                                        e.getMessage()), e);
                return null;
            }
        }
    }

    public int getOrCreateSupplier(String interceptorClassName,
                                   ClassLoader classLoader) {
        Map<String, InterceptorSupplier> suppliers = interceptorMaps.computeIfAbsent(interceptorClassName, (v) -> new ConcurrentHashMap<>());

        String classLoaderId = classLoader == null ? "bootstrap" : classLoader.getClass().getName() + "@" + System.identityHashCode(classLoader);
        InterceptorSupplier supplier = suppliers.get(classLoaderId);
        if (supplier != null) {
            return supplier.index;
        }

        synchronized (InterceptorManager.class) {
            // Double check
            supplier = suppliers.get(classLoaderId);
            if (supplier != null) {
                return supplier.index;
            }

            ensureCapacity(interceptorIndex);
            int index = interceptorIndex++;

            supplier = new InterceptorSupplier(index, interceptorClassName, classLoader);
            suppliers.put(classLoaderId, supplier);

            interceptorList[index] = supplier;
        }

        return supplier.index;
    }

    /**
     * Take a snapshot of suppliers
     */
    public Map<String, InterceptorSupplier> getSuppliers(String interceptorClazz) {
        return Collections.unmodifiableMap(new HashMap<>(this.interceptorMaps.getOrDefault(interceptorClazz, Collections.emptyMap())));
    }

    private void ensureCapacity(int index) {
        if (index < interceptorList.length) {
            return;
        }

        InterceptorSupplier[] newArray = new InterceptorSupplier[(int) (interceptorList.length * 1.5)];
        System.arraycopy(interceptorList, 0, newArray, 0, interceptorList.length);
        interceptorList = newArray;
        LOG.info("Enlarge dynamic interceptors storage to {}", interceptorList.length);
    }
}
