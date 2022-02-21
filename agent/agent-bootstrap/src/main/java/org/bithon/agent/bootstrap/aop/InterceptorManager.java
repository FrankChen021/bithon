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

package org.bithon.agent.bootstrap.aop;


import org.bithon.agent.bootstrap.loader.PluginClassLoaderManager;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Interceptor is singleton
 *
 * @author frankchen
 */
public class InterceptorManager {
    private static final IAopLogger log = BootstrapHelper.createAopLogger(InterceptorManager.class);

    private static final Map<String, AbstractInterceptor> INTERCEPTORS = new ConcurrentHashMap<>();

    private static final ReentrantLock INTERCEPTOR_INSTANTIATION_LOCK = new ReentrantLock();

    public static AbstractInterceptor getInterceptor(String interceptorClassName, Class<?> fromClass) {
        // get interceptor from cache first
        String interceptorId = generateInterceptorId(interceptorClassName, fromClass.getClassLoader());
        AbstractInterceptor interceptor = INTERCEPTORS.get(interceptorId);
        if (interceptor != null) {
            return interceptor;
        }

        try {
            // load class out of lock in case of dead lock
            ClassLoader interceptorClassLoader = PluginClassLoaderManager.getClassLoader(fromClass.getClassLoader());
            Class<?> interceptorClass = Class.forName(interceptorClassName, true, interceptorClassLoader);

            INTERCEPTOR_INSTANTIATION_LOCK.lock();
            try {
                interceptor = INTERCEPTORS.get(interceptorId);
                if (interceptor != null) {
                    // double check
                    return interceptor;
                }

                interceptor = (AbstractInterceptor) interceptorClass.newInstance();
                if (!interceptor.initialize()) {
                    log.warn("Interceptor not loaded for failure of initialization: [%s]", null);
                    return null;
                }

                INTERCEPTORS.put(interceptorId, interceptor);
                return interceptor;
            } finally {
                INTERCEPTOR_INSTANTIATION_LOCK.unlock();
            }
        } catch (Throwable e) {
            log.error(String.format(Locale.ENGLISH,
                                    "Failed to load interceptor[%s] due to %s",
                                    interceptorClassName,
                                    e.getMessage()), e);
            return null;
        }
    }

    private static String generateInterceptorId(String interceptorClass,
                                                ClassLoader loader) {
        if (null == loader) {
            return "bootstrap-" + interceptorClass;
        }
        return loader.hashCode() + "-" + interceptorClass;
    }

}
