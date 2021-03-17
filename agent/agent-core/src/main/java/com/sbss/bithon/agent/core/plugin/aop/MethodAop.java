package com.sbss.bithon.agent.core.plugin.aop;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AroundMethodAop;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IAopLogger;
import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.SuperCall;
import shaded.net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @author frankchen
 * @date 2020-12-31 22:22:05
 */
public class MethodAop {

    private static IAopLogger aopLogger = AopLogger.getLogger(MethodAop.class);

    private final AbstractInterceptor interceptor;

    public MethodAop(AbstractInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @RuntimeType
    public Object intercept(@Origin Class<?> clazz,
                            @SuperCall Callable<?> superMethod,
                            @This(optional = true) Object target,
                            @Origin Method method,
                            @AllArguments Object[] args) throws Exception {
        return AroundMethodAop.intercept(aopLogger,
                                         interceptor,
                                         clazz,
                                         superMethod,
                                         target,
                                         method,
                                         args);
    }
}
