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

package com.sbss.bithon.agent.boot.aop;

import java.lang.reflect.Method;

/**
 * @author frankchen
 * @date 2020-12-31 22:22:05
 */
public final class AroundMethodAop {

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
