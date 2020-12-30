package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
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
public class PluginInterceptorManager {
    private static final Logger log = LoggerFactory.getLogger(PluginInterceptorManager.class);

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
        ClassLoader interceptorClassLoader = AgentClassloaderManager.getAgentLoader(classLoader);
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
                log.warn("Failed to initialize {}, interceptor skipped", interceptor.getClass().getSimpleName());
                return null;
            }

            log.info("Interceptor {}.{} Loaded", plugin.getClass().getSimpleName(), interceptor.getClass().getSimpleName());
            INTERCEPTORS.put(interceptorId, interceptor);
            return interceptor;
        } finally {
            INTERCEPTOR_INSTANTIATION_LOCK.unlock();
        }
    }

    static void loadInterceptor(String interceptorClassName) throws Exception {
        ClassLoader interceptorClassLoader = AgentClassloader.getDefaultInstance();
        String interceptorId = generateInterceptorId(interceptorClassName, interceptorClassLoader);

        Class<?> interceptorClass = Class.forName(interceptorClassName,
                                                  true,
                                                  interceptorClassLoader);

        INTERCEPTOR_INSTANTIATION_LOCK.lock();
        try {
            AbstractInterceptor interceptor = INTERCEPTORS.get(interceptorId);
            if (interceptor == null) {
                interceptor = (AbstractInterceptor) interceptorClass.newInstance();
                if (interceptor.initialize()) {
                    log.info("{} initialized", interceptor.getClass().getSimpleName());
                    INTERCEPTORS.put(interceptorId, interceptor);
                }
            }
        } finally {
            INTERCEPTOR_INSTANTIATION_LOCK.unlock();
        }
    }

    private static String generateInterceptorId(String interceptorClass,
                                                ClassLoader loader) {
        if (null == loader) {
            return "bootstrap" + interceptorClass;
        }
        return loader.hashCode() + interceptorClass;
    }

}
