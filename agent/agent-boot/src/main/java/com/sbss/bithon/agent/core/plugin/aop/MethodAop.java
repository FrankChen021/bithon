package com.sbss.bithon.agent.core.plugin.aop;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AroundMethodAop;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.BootstrapHelper;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IAopLogger;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.ISuperMethod;
import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Morph;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;

/**
 * @author frankchen
 * @date 2020-12-31 22:22:05
 */
public class MethodAop {

    private static final IAopLogger aopLogger = BootstrapHelper.createAopLogger(MethodAop.class);

    private final Object interceptor;

    public MethodAop(Object interceptor) {
        this.interceptor = interceptor;
    }

    @RuntimeType
    public Object intercept(@Origin Class<?> targetClass,
                            @Origin Method targetMethod,
                            @Morph ISuperMethod superMethod,
                            @This(optional = true) Object targetObject,
                            @AllArguments Object[] args) throws Exception {
        return AroundMethodAop.intercept(aopLogger,
                                         (AbstractInterceptor)interceptor,
                                         targetClass,
                                         superMethod,
                                         targetObject,
                                         targetMethod,
                                         args);
    }
}
