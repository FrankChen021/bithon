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

import org.bithon.agent.instrumentation.aop.context.AopContextImpl;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AbstractInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.util.Locale;


/**
 * @author frankchen
 * @date 2021-02-18 20:20
 */
public class AfterAdvice {
    public static final ILogger LOG = LoggerFactory.getLogger(AfterAdvice.class);

    /**
     * This method is only used for byte-buddy method advice. Have no use during the execution since the code has been injected into target class.
     */
    @Advice.OnMethodEnter
    public static void onEnter(@Advice.This(optional = true) Object target,
                               @Advice.Origin Class<?> clazz,
                               @Advice.Origin("#m") String method,
                               @Advice.AllArguments Object[] args,
                               @Advice.Local("context") Object context) {
        AopContextImpl aopContext = new AopContextImpl(clazz, method, target, args);
        aopContext.onBeforeTargetMethodInvocation();

        // Assign the context so that the leave method can access this object
        context = aopContext;
    }

    /**
     * This method is only used for byte-buddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@AdviceAnnotation.InterceptorIndex int index,
                              @AdviceAnnotation.InterceptorName String name,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returning,
                              @Advice.Thrown Throwable exception,
                              @Advice.Local("context") Object context) {

        AbstractInterceptor interceptor = InterceptorManager.INSTANCE.getInterceptor(index);
        if (interceptor == null) {
            return;
        }

        AopContextImpl aopContext = (AopContextImpl) context;
        aopContext.onAfterTargetMethodInvocation();
        aopContext.setException(exception);
        aopContext.setReturning(returning);

        try {
            interceptor.hit();
            ((AfterInterceptor) interceptor).after(aopContext);
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Exception occurred when executing onExit of [%s] for [%s]: %s",
                                    name,
                                    aopContext.getTargetClass().getSimpleName(),
                                    e.getMessage()),
                      e);

            interceptor.exception(e);
        }

        returning = aopContext.getReturning();
    }
}
