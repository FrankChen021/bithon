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

package org.bithon.agent.bootstrap.aop.advice;

import org.bithon.agent.bootstrap.aop.BootstrapHelper;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Classes of spring beans are re-transformed after these classes are loaded,
 * so we HAVE to use {@link Advice} instead of {@link shaded.net.bytebuddy.implementation.MethodDelegation}to intercept methods
 * <p>
 * And because the byte code of the methods are weaved into target classes which are loaded by many different class loaders,
 * we also HAVE to inject any dependencies to the bootstrap class loader so that they could be found via any class loader
 *
 * @author frank.chen021@outlook.com
 * @date 2021/12/29 22:19
 */
public class IAdviceAopTemplate {

    // assigned by class generator
    private static String INTERCEPTOR_CLASS_NAME;

    private static volatile IAdviceInterceptor interceptorInstance;

    public static IAdviceInterceptor getOrCreateInterceptor() {
        if (interceptorInstance != null) {
            return interceptorInstance;
        }

        try {
            // load class out of sync to eliminate potential dead lock
            Class<?> interceptorClass = Class.forName(INTERCEPTOR_CLASS_NAME,
                                                      true,
                                                      BootstrapHelper.getPluginClassLoader());
            synchronized (INTERCEPTOR_CLASS_NAME) {
                //double check
                if (interceptorInstance != null) {
                    return interceptorInstance;
                }

                interceptorInstance = (IAdviceInterceptor) interceptorClass.newInstance();
            }

        } catch (Exception e) {
            BootstrapHelper.createAopLogger(IAdviceAopTemplate.class)
                           .error(String.format(Locale.ENGLISH, "Failed to create interceptor [%s]", INTERCEPTOR_CLASS_NAME), e);
        }
        return interceptorInstance;
    }

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodEnter
    public static void enter(
        final @Advice.Origin Method method,
        final @Advice.This(optional = true) Object target,
        final @Advice.AllArguments Object[] args,
        @Advice.Local("context") Object context
    ) {
        IAdviceInterceptor interceptor = getOrCreateInterceptor();
        if (interceptor != null) {
            context = interceptor.onMethodEnter(method, target, args);
        }
    }

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(final @Advice.Origin Method method,
                            final @Advice.This Object target,
                            @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returning,
                            final @Advice.AllArguments Object[] args,
                            final @Advice.Thrown Throwable exception,
                            final @Advice.Local("context") Object context) {
        if (context != null) {
            IAdviceInterceptor interceptor = getOrCreateInterceptor();
            if (interceptor != null) {
                returning = interceptor.onMethodExit(method, target, args, returning, exception, context);
            }
        }
    }
}
