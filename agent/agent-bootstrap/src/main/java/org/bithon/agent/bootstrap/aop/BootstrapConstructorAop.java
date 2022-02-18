/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.bootstrap.aop;


import org.bithon.agent.bootstrap.aop.advice.InterceptorManager;
import org.bithon.agent.bootstrap.aop.bytebuddy.Interceptor;
import shaded.net.bytebuddy.asm.Advice;

import java.lang.reflect.Constructor;


/**
 * @author frankchen
 * @date 2021-02-18 18:03
 */
public class BootstrapConstructorAop {
    private static final IAopLogger log = BootstrapHelper.createAopLogger(BootstrapMethodAop.class);

    /*
    @RuntimeType
    public static void onConstruct(@Interceptor String interceptorClassName,
                                   @Origin Class<?> targetClass,
                                   @Origin Constructor<?> constructor,
                                   @This Object targetObject,
                                   @AllArguments Object[] args) {
        try {
            AbstractInterceptor interceptor = InterceptorManager.getOrCreateInterceptor(interceptorClassName);
            if (interceptor == null) {
                return;
            }
            interceptor.onConstruct(new AopContext(targetClass, constructor, targetObject, args));
        } catch (Throwable e) {
            if (log != null) {
                log.error(String.format(Locale.ENGLISH,
                                        "Failed to invoke onConstruct interceptor[%s]",
                                        interceptorClassName),
                          e);
            } else {
                e.printStackTrace();
            }
        }
    }*/

    @Advice.OnMethodExit
    public static void enter(final @Interceptor String interceptorClassName,
                             final @Advice.Origin Constructor method,
                             final @Advice.This Object target,
                             @Advice.AllArguments Object[] args) throws Exception {
        AbstractInterceptor interceptor = InterceptorManager.getOrCreateInterceptor(interceptorClassName);
        if (interceptor == null) {
            return;
        }
        interceptor.onConstruct(new AopContext(method.getClass(), method, target, args));
    }
}
