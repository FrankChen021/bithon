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

package org.bithon.agent.instrumentation.aop.advice;

import org.bithon.agent.instrumentation.aop.interceptor.IDynamicInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Classes of spring beans are re-transformed after these classes are loaded,
 * so we HAVE to use {@link Advice} instead of {@link org.bithon.shaded.net.bytebuddy.implementation.MethodDelegation}to intercept methods
 * <p>
 * And because the byte code of the methods are weaved into target classes which are loaded by many class loaders,
 * we also HAVE to inject any dependencies to the bootstrap class loader so that they could be found via any class loader
 *
 * @author frank.chen021@outlook.com
 * @date 2021/12/29 22:19
 */
public class DynamicAopAdvice {

    private static final ILogger LOG = LoggerFactory.getLogger(DynamicAopAdvice.class);

    /**
     * This method is only used for byte-buddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodEnter
    public static void onEnter(
            @AdviceAnnotation.InterceptorName String name,
            @AdviceAnnotation.InterceptorIndex int index,
            @Advice.Origin Method method,
            @Advice.This(optional = true) Object target,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Local("context") Object context,
            @Advice.Local("interceptor") Object interceptor
    ) {
        interceptor = InterceptorManager.getInterceptor(index);
        if (interceptor != null) {
            Object[] newArgs = args;

            try {
                context = ((IDynamicInterceptor) interceptor).onMethodEnter(method, target, newArgs);
            } catch (Throwable t) {
                LOG.error(String.format(Locale.ENGLISH, "Failed to execute interceptor [%s]", name), t);
                return;
            }

            // This assignment must be kept since it tells byte-buddy that args might have been re-written
            // so that byte-buddy re-map the args to original function input argument
            args = newArgs;
        }
    }

    /**
     * This method is only used for byte-buddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin Method method,
                              @Advice.This Object target,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returning,
                              @Advice.AllArguments Object[] args,
                              @Advice.Thrown Throwable exception,
                              @Advice.Local("context") Object context,
                              @Advice.Local("interceptor") Object interceptor) {
        if (context == null || interceptor == null) {
            return;
        }
        try {
            returning = ((IDynamicInterceptor) interceptor).onMethodExit(method, target, args, returning, exception, context);
        } catch (Throwable t) {
            LOG.error(String.format(Locale.ENGLISH, "Failed to execute exit interceptor [%s]", interceptor.getClass().getName()), t);
        }
    }
}
