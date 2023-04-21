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
import java.util.function.Supplier;

/**
 * Interceptor is singleton
 *
 * @author frankchen
 */
public class InterceptorManager {

    private static final ILogger LOG = LoggerFactory.getLogger(InterceptorManager.class);

    private static InterceptorLazySupplier[] ARRAY_INTERCEPTORS = new InterceptorLazySupplier[32];
    private static final Map<String, InterceptorLazySupplier> INTERCEPTORS = new ConcurrentHashMap<>();

    private static int INDEX = 0;

    /**
     * Get interceptor by given index.
     */
    public static Supplier<IInterceptor> getInterceptor(int index) {
        return ARRAY_INTERCEPTORS[index];
    }

    public static class InterceptorLazySupplier implements Supplier<IInterceptor> {
        private String interceptorClassName;
        private ClassLoader classLoader;

        private volatile IInterceptor interceptor;
        private final int index;

        public InterceptorLazySupplier(int index,
                                       String interceptorClassName,
                                       ClassLoader classLoader) {
            this.index = index;
            this.interceptorClassName = interceptorClassName;
            this.classLoader = classLoader;
        }

        @Override
        public IInterceptor get() {
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
            }
            return interceptor;
        }

        private IInterceptor createInterceptor(String interceptorClassName, ClassLoader userClassLoader) {
            try {
                // Load class out of lock in case of deadlock
                ClassLoader interceptorClassLoader = PluginClassLoaderManager.getClassLoader(userClassLoader);
                Class<?> interceptorClass = Class.forName(interceptorClassName, true, interceptorClassLoader);

                return (IInterceptor) interceptorClass.getConstructor().newInstance();
            } catch (Throwable e) {
                LOG.error(String.format(Locale.ENGLISH,
                                        "Failed to load interceptor[%s] due to %s",
                                        interceptorClassName,
                                        e.getMessage()), e);
                return null;
            }
        }
    }

    public static int getOrCreateInterceptorSupplier(String interceptorClassName,
                                                     ClassLoader classLoader) {
        // Get interceptor from cache first
        String interceptorId = generateInterceptorId(interceptorClassName, classLoader);

        InterceptorLazySupplier supplier = INTERCEPTORS.get(interceptorId);
        if (supplier != null) {
            return supplier.index;
        }

        synchronized (InterceptorManager.class) {
            // Double check
            supplier = INTERCEPTORS.get(interceptorId);
            if (supplier != null) {
                return supplier.index;
            }

            ensureCapacity(INDEX);
            int index = INDEX++;

            supplier = new InterceptorLazySupplier(index, interceptorClassName, classLoader);
            INTERCEPTORS.put(interceptorId, supplier);

            ARRAY_INTERCEPTORS[index] = supplier;
        }

        return supplier.index;
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

        InterceptorLazySupplier[] newArray = new InterceptorLazySupplier[(int) (ARRAY_INTERCEPTORS.length * 1.5)];
        System.arraycopy(ARRAY_INTERCEPTORS, 0, newArray, 0, ARRAY_INTERCEPTORS.length);
        ARRAY_INTERCEPTORS = newArray;
        LOG.info("Enlarge dynamic interceptors storage to {}", ARRAY_INTERCEPTORS.length);
    }
}
