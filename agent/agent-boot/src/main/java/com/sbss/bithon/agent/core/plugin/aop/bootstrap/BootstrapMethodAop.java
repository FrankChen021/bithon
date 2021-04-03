package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

import com.sbss.bithon.agent.dependency.AgentDependencyManager;
import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Morph;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;
import shaded.net.bytebuddy.pool.TypePool;

import java.lang.reflect.Method;
import java.util.Map;


/**
 * @author frankchen
 * @date 2021-02-18 20:20
 */
public class BootstrapMethodAop {
    /**
     * assigned by {@link com.sbss.bithon.agent.core.plugin.loader.BootstrapInterceptorInstaller#generateAopClass(Map, TypePool, String, String, com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptor)}
     */
    private static String INTERCEPTOR_CLASS_NAME;

    private static AbstractInterceptor INTERCEPTOR;
    private static IAopLogger log;

    @RuntimeType
    public static Object intercept(@Origin Class<?> targetClass,
                                   @Morph ISuperMethod superMethod,
                                   @This(optional = true) Object target,
                                   @Origin Method method,
                                   @AllArguments Object[] args) throws Exception {
        AbstractInterceptor interceptor = ensureInterceptor();
        if (interceptor == null) {
            return superMethod.invoke(args);
        }

        return AroundMethodAop.intercept(log,
                                         INTERCEPTOR,
                                         targetClass,
                                         superMethod,
                                         target,
                                         method,
                                         args);
    }

    private static AbstractInterceptor ensureInterceptor() {
        if (INTERCEPTOR != null) {
            return INTERCEPTOR;
        }

        log = BootstrapHelper.createAopLogger(BootstrapMethodAop.class);

        try {
            // load class out of sync to eliminate potential dead lock
            Class<?> interceptorClass = Class.forName(INTERCEPTOR_CLASS_NAME,
                                                      true,
                                                      AgentDependencyManager.getClassLoader());
            synchronized (INTERCEPTOR_CLASS_NAME) {
                //double check
                if (INTERCEPTOR != null) {
                    return INTERCEPTOR;
                }

                INTERCEPTOR = (AbstractInterceptor) interceptorClass.newInstance();
            }
            INTERCEPTOR.initialize();

        } catch (Exception e) {
            log.error(String.format("Failed to instantiate interceptor [%s]", INTERCEPTOR_CLASS_NAME), e);
        }
        return INTERCEPTOR;
    }
}

