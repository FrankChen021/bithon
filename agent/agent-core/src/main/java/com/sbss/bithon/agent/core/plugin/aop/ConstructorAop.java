package com.sbss.bithon.agent.core.plugin.aop;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * @author frankchen
 * @date 2020-12-31 22:21:36
 */
public class ConstructorAop {
    private static final Logger log = LoggerFactory.getLogger(ConstructorAop.class);

    private final AbstractInterceptor interceptor;

    public ConstructorAop(AbstractInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @RuntimeType
    public void onConstruct(@Origin Class<?> targetClass,
                            @Origin Constructor<?> constructor,
                            @This Object targetObject,
                            @AllArguments Object[] args) {
        try {
            interceptor.onConstruct(new AopContext(targetClass, constructor, targetObject, args));
        } catch (Exception e) {
            log.error(String.format("Error occurred during invoking %s.onConstruct()",
                                    interceptor.getClass().getSimpleName()),
                      e);
        }
    }
}
