/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.plugin.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.PluginClassLoaderManager;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Interceptor is singleton
 *
 * @author frankchen
 */
class InterceptorManager {
    private static final Logger log = LoggerFactory.getLogger(InterceptorManager.class);

    private static final Map<String, AbstractInterceptor> INTERCEPTORS = new ConcurrentHashMap<>();

    private static final ReentrantLock INTERCEPTOR_INSTANTIATION_LOCK = new ReentrantLock();

    static AbstractInterceptor loadInterceptor(AbstractPlugin plugin,
                                               String interceptorClassName,
                                               ClassLoader classLoader) throws Exception {
        // get interceptor from cache first
        String interceptorId = generateInterceptorId(interceptorClassName, classLoader);
        AbstractInterceptor interceptor = INTERCEPTORS.get(interceptorId);
        if (interceptor != null) {
            return interceptor;
        }

        // load class out of lock in case of dead lock
        ClassLoader interceptorClassLoader = PluginClassLoaderManager.getClassLoader(classLoader);
        Class<?> interceptorClass = Class.forName(interceptorClassName, true, interceptorClassLoader);

        String interceptorName = getInterceptorName(interceptorClass);
        INTERCEPTOR_INSTANTIATION_LOCK.lock();
        try {
            interceptor = INTERCEPTORS.get(interceptorId);
            if (interceptor != null) {
                // double check
                return interceptor;
            }

            interceptor = (AbstractInterceptor) interceptorClass.newInstance();
            if (!interceptor.initialize()) {
                log.warn("Interceptor not loaded for failure of initialization: [{}.{}]",
                         plugin.getClass().getSimpleName(),
                         interceptorName);
                return null;
            }

            log.info("Loaded interceptor [{}.{}]",
                     plugin.getClass().getSimpleName(),
                     interceptorName);
            INTERCEPTORS.put(interceptorId, interceptor);
            return interceptor;
        } finally {
            INTERCEPTOR_INSTANTIATION_LOCK.unlock();
        }
    }

    private static String getInterceptorName(Class<?> interceptorClass) {
        String name = interceptorClass.getName();
        int dot = name.lastIndexOf('.');
        return dot == -1 ? name : name.substring(dot + 1);
    }

    private static String generateInterceptorId(String interceptorClass,
                                                ClassLoader loader) {
        if (null == loader) {
            return "bootstrap-" + interceptorClass;
        }
        return loader.hashCode() + "-" + interceptorClass;
    }

}
