package com.sbss.bithon.agent.core.interceptor;

import shaded.net.bytebuddy.implementation.bind.annotation.AllArguments;
import shaded.net.bytebuddy.implementation.bind.annotation.Origin;
import shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType;
import shaded.net.bytebuddy.implementation.bind.annotation.SuperCall;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Description : 针对于静态方法的切面增强, 和一般切面不同的是, 静态方法由于加载时机和一般方法不同, 其增强实际上是对class的重写
 * <br>
 * Date: 17/11/9
 *
 * @author 马至远
 */
public class StaticMethodInterceptor implements IMethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StaticMethodInterceptor.class);

    private AbstractMethodIntercepted eventCallback;

    @RuntimeType
    public Object intercept(@Origin Class<?> clazz,
                            @SuperCall Callable<?> superMethod,
                            @Origin Method method,
                            @AllArguments Object[] args) throws Exception {
        Object result = null;
        Exception exception = null;
        Object context = null;
        try {
            // class增强不关心实例, 只需要获取到方法名和参数
            BeforeJoinPoint beforeJoinPoint = new BeforeJoinPoint(null, method, args);
            eventCallback.before(beforeJoinPoint);
            context = eventCallback.createContext(beforeJoinPoint);
        } catch (Exception e) {
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
            // class增强不关心实例, 只需要获取到方法名和参数
            eventCallback.after(new AfterJoinPoint(null, method, args, context, result, exception));
        } catch (Exception e) {
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
