package com.sbss.bithon.agent.core.interceptor;

import shaded.net.bytebuddy.implementation.bind.annotation.*;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * 针对一般方法的切面增强
 *
 * @author lizheng
 * @author mazy
 */
public class AroundMethodInterceptor implements IMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AroundMethodInterceptor.class);

    private AbstractMethodIntercepted eventCallback;

    @RuntimeType
    public Object intercept(@SuperCall Callable<?> superMethod,
                            @This Object target,
                            @Origin Method method,
                            @AllArguments Object[] args) throws Exception {
        Object result = null;
        Exception exception = null;
        Object context = null;
        try {
            BeforeJoinPoint beforeJoinPoint = new BeforeJoinPoint(target, method, args);
            eventCallback.before(beforeJoinPoint);
            context = eventCallback.createContext(beforeJoinPoint);
        } catch (Throwable e) {
            log.error(String.format("Error occurred during invoking %s.before()",
                                    eventCallback.getClass().getSimpleName()),
                      e);
        }
        try {
            result = superMethod.call();
        } catch (Exception e) {
            exception = e;
        }
        try {
            eventCallback.after(new AfterJoinPoint(target, method, args, context, result, exception));
        } catch (Throwable e) {
            log.error(String.format("Error occurred during invoking %s.after()",
                                    eventCallback.getClass().getSimpleName()),
                      e);
        }
        if (null != exception) {
            throw exception;
        }
        return result;
    }

    public void setEventCallback(AbstractMethodIntercepted eventCallback) {
        this.eventCallback = eventCallback;
    }
}
