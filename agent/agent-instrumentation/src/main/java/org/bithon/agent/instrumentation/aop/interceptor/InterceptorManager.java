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
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor is singleton
 *
 * @author frankchen
 */
public class InterceptorManager {

    public static final InterceptorManager INSTANCE = new InterceptorManager();

    private int globalInterceptorIndex = 0;

    private InterceptorSupplier[] interceptorList = new InterceptorSupplier[256];

    /**
     * key - interceptor class name
     * val - interceptor instance based on target class's class loader
     * key - System.identityHashCode of class loader
     * val - interceptor instance
     */
    private final Map<String, Map<String, InterceptorSupplier>> interceptorMaps = new ConcurrentHashMap<>();

    public InterceptorSupplier getSupplier(int index) {
        return index < interceptorList.length ? interceptorList[index] : null;
    }

    /**
     * Get interceptor by given index.
     */
    public AbstractInterceptor getInterceptor(int index) {
        if (index < interceptorList.length) {
            InterceptorSupplier supplier = interceptorList[index];

            //
            // The case that the returned supplier is null is very complicated.
            // If the target application runs with agent while it enables the spring-boot-devtools,
            // this InterceptorManager class will be loaded into multiple class loaders.
            // However, only one of the loaders has the interceptorList assigned.
            //
            if (supplier != null) {
                return supplier.get();
            }
        }
        return null;
    }

    /**
     * Get or create an interceptor supplier
     *
     * @return the global index of this supplier so that it can be passed to {{@link #getInterceptor(int)}} to get the supplier instance
     */
    public int getOrCreateSupplier(String interceptorClassName,
                                   ClassLoader classLoader) {
        Map<String, InterceptorSupplier> suppliers = interceptorMaps.computeIfAbsent(interceptorClassName, (v) -> new ConcurrentHashMap<>());

        String classLoaderId = classLoader == null ? "bootstrap" : classLoader.getClass().getName() + "@" + System.identityHashCode(classLoader);
        InterceptorSupplier supplier = suppliers.get(classLoaderId);
        if (supplier != null) {
            return supplier.getIndex();
        }

        synchronized (this) {
            // Double check
            supplier = suppliers.get(classLoaderId);
            if (supplier != null) {
                return supplier.getIndex();
            }

            ensureCapacity(globalInterceptorIndex);
            int index = globalInterceptorIndex++;

            supplier = new InterceptorSupplier(index, interceptorClassName, classLoader, classLoaderId);
            suppliers.put(classLoaderId, supplier);

            interceptorList[index] = supplier;
        }

        return supplier.getIndex();
    }

    /**
     * Get installed interceptors by a given interceptor.
     * An interceptor might be installed to multiple methods in same/different classes.
     *
     * @return if target interceptor does not exist, an empty map, instead of null, is returned
     */
    public Map<String, InterceptorSupplier> getSuppliers(String interceptorClazz) {
        return Collections.unmodifiableMap(new HashMap<>(this.interceptorMaps.getOrDefault(interceptorClazz, Collections.emptyMap())));
    }

    private void ensureCapacity(int index) {
        if (index < interceptorList.length) {
            return;
        }

        int oldCapacity = interceptorList.length;
        int newCapacity = (int) (oldCapacity * 1.5);
        InterceptorSupplier[] newArray = new InterceptorSupplier[newCapacity];
        System.arraycopy(interceptorList, 0, newArray, 0, oldCapacity);
        interceptorList = newArray;
        LoggerFactory.getLogger(InterceptorManager.class)
                     .info("Enlarge interceptors storage from {} to {}", oldCapacity, newCapacity);
    }
}
