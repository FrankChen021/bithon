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


import org.bithon.agent.instrumentation.loader.PluginClassLoaderManager;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Interceptor is singleton
 *
 * @author frankchen
 */
public class InterceptorManager {
    public static class InterceptorEntry {
        public final IInterceptor interceptor;

        /**
         * Index in the array.
         * -1 means no index for current entry
         */
        public final int index;

        public InterceptorEntry(IInterceptor interceptor, int index) {
            this.interceptor = interceptor;
            this.index = index;
        }
    }

    private static final ILogger LOG = LoggerFactory.getLogger(InterceptorManager.class);

    private static IInterceptor[] ARRAY_INTERCEPTORS = new IInterceptor[4];
    private static final Map<String, InterceptorEntry> INTERCEPTORS = new ConcurrentHashMap<>();
    private static final ReentrantLock INTERCEPTOR_INSTANTIATION_LOCK = new ReentrantLock();

    private static final AtomicInteger INDEX = new AtomicInteger(0);

    /**
     * Get interceptor by given index.
     */
    public static IDynamicInterceptor getInterceptor(int index) {
        return (IDynamicInterceptor) ARRAY_INTERCEPTORS[index];
    }

    /**
     * Called by injected code in static initializer for target classes
     * see {@link org.bithon.agent.instrumentation.aop.interceptor.installer.InterceptorInstaller} for more detail
     */
    public static IInterceptor getInterceptor(String interceptorClassName, Class<?> fromClass) {
        InterceptorEntry entry = getOrCreateInterceptor(interceptorClassName, fromClass.getClassLoader(), false);
        return entry == null ? null : entry.interceptor;
    }

    public static InterceptorEntry getOrCreateInterceptor(String interceptorClassName,
                                                          ClassLoader classLoader,
                                                          boolean createIndex) {
        // Get interceptor from cache first
        String interceptorId = generateInterceptorId(interceptorClassName, classLoader);
        InterceptorEntry entry = INTERCEPTORS.get(interceptorId);
        if (entry != null) {
            return entry;
        }

        try {
            // Load class out of lock in case of deadlock
            ClassLoader interceptorClassLoader = PluginClassLoaderManager.getClassLoader(classLoader);
            Class<?> interceptorClass = Class.forName(interceptorClassName, true, interceptorClassLoader);

            INTERCEPTOR_INSTANTIATION_LOCK.lock();
            try {
                entry = INTERCEPTORS.get(interceptorId);
                if (entry != null) {
                    // Double check
                    return entry;
                }

                IInterceptor interceptor = (IInterceptor) interceptorClass.getConstructor().newInstance();
                int index = -1;
                if (createIndex) {
                    index = INDEX.getAndIncrement();
                    ensureCapacity(index);
                    ARRAY_INTERCEPTORS[index] = interceptor;
                }

                entry = new InterceptorEntry(interceptor, index);
                INTERCEPTORS.put(interceptorId, entry);
                return entry;
            } finally {
                INTERCEPTOR_INSTANTIATION_LOCK.unlock();
            }
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH,
                                    "Failed to load interceptor[%s] due to %s",
                                    interceptorClassName,
                                    e.getMessage()), e);
            return null;
        }
    }

    private static String generateInterceptorId(String interceptorClass,
                                                ClassLoader loader) {
        if (null == loader) {
            return interceptorClass + "@bootstrap";
        }
        return interceptorClass + "@" + System.identityHashCode(loader);
    }

    private static void ensureCapacity(int index) {
        if (ARRAY_INTERCEPTORS.length > index) {
            return;
        }

        IInterceptor[] newArray = new IDynamicInterceptor[(int) (ARRAY_INTERCEPTORS.length * 1.5)];
        System.arraycopy(ARRAY_INTERCEPTORS, 0, newArray, 0, ARRAY_INTERCEPTORS.length);
        ARRAY_INTERCEPTORS = newArray;
        LOG.info("Enlarge dynamic interceptors storage to {}", ARRAY_INTERCEPTORS.length);
    }
}
