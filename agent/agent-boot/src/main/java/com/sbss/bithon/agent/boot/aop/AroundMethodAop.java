package com.sbss.bithon.agent.boot.aop;

import java.lang.reflect.Method;

/**
 * @author frankchen
 * @date 2020-12-31 22:22:05
 */
final public class AroundMethodAop {

    public static Object intercept(IAopLogger log,
                                   AbstractInterceptor interceptor,
                                   Class<?> targetClass,
                                   ISuperMethod superMethod,
                                   Object target,
                                   Method method,
                                   Object[] args) throws Exception {
        AopContext context = new AopContext(targetClass, method, target, args);

        //
        // before execution of intercepted method
        //
        {
            InterceptionDecision decision = InterceptionDecision.CONTINUE;
            try {
                decision = interceptor.onMethodEnter(context);
            } catch (Throwable e) {
                log.warn(String.format("Error occurred during invoking %s.before()",
                                       interceptor.getClass().getSimpleName()),
                         e);

                //continue execution
            }

            if (InterceptionDecision.SKIP_LEAVE.equals(decision)) {
                return superMethod.invoke(args);
            }
        }

        //
        // call intercepted method
        //
        long startTime = System.nanoTime();
        Object returning = null;
        Exception exception = null;
        {
            try {
                returning = superMethod.invoke(args);
            } catch (Exception e) {
                exception = e;
            }
        }
        context.setCostTime(System.nanoTime() - startTime);
        context.setEndTimestamp(System.currentTimeMillis());

        //
        // after execution of intercepted method
        //
        {
            try {
                context.setException(exception);
                context.setReturning(returning);
                interceptor.onMethodLeave(context);
            } catch (Throwable e) {
                log.warn(String.format("Error occurred during invoking %s.after()",
                                       interceptor.getClass().getSimpleName()),
                         e);
            }
        }

        if (null != exception) {
            throw exception;
        }
        return returning;
    }
}
