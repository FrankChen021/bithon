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

package com.sbss.bithon.agent.plugin.spring.mvc;

import com.sbss.bithon.agent.bootstrap.aop.BootstrapHelper;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;

/**
 * Classes of spring beans are re-transformed after these classes are loaded,
 * so we have to use {@link Advice} to intercept methods
 * <p>
 * NOTE:
 * This class will be injected into bootstrap class loader as 'SpringBeanMethodAopInBootstrap',
 * ALL its dependencies must be in bootstrap class loader too
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 16:45
 */
public class SpringBeanMethodAop {

    /**
     * use a static variable to make sure referenced classes of this object could be found via correct class loader
     */
    public static String interceptorClassName;

    private static SpringBeanMethodInterceptorIntf interceptorInstance;

    @Advice.OnMethodEnter
    public static void enter(
        final @Advice.Origin Method method,
        final @Advice.This(optional = true) Object target,
        final @Advice.AllArguments Object[] args,
        @Advice.Local("context") Object context
    ) {
        SpringBeanMethodInterceptorIntf interceptor = ensureInterceptor();
        if (interceptor != null) {
            context = interceptor.onMethodEnter(method, target, args);
        }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(final @Advice.Origin Method method,
                            final @Advice.This Object target,
                            final @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returning,
                            final @Advice.AllArguments Object[] args,
                            final @Advice.Thrown Throwable exception,
                            final @Advice.Local("context") Object context) {
        if (context != null) {
            SpringBeanMethodInterceptorIntf interceptor = ensureInterceptor();
            if (interceptor != null) {
                interceptor.onMethodExit(method, target, args, exception, context);
            }
        }
    }

    /**
     * this must be public
     */
    public static SpringBeanMethodInterceptorIntf ensureInterceptor() {
        if (interceptorInstance != null) {
            return interceptorInstance;
        }


        try {
            // load class out of sync to eliminate potential dead lock
            Class<?> interceptorClass = Class.forName(interceptorClassName,
                                                      true,
                                                      BootstrapHelper.getPluginClassLoader());
            synchronized (interceptorClassName) {
                //double check
                if (interceptorInstance != null) {
                    return interceptorInstance;
                }

                interceptorInstance = (SpringBeanMethodInterceptorIntf) interceptorClass.newInstance();
            }

        } catch (Exception e) {
            BootstrapHelper.createAopLogger(SpringBeanMethodAop.class)
                           .error(String.format("Failed to create interceptor [%s]", interceptorClassName), e);
        }
        return interceptorInstance;
    }
}
