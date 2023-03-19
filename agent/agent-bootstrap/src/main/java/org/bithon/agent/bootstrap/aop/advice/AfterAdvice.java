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

import org.bithon.agent.bootstrap.aop.AopContextImpl;
import org.bithon.agent.bootstrap.aop.BootstrapHelper;
import org.bithon.agent.bootstrap.aop.IAopLogger;
import org.bithon.agent.bootstrap.aop.interceptor.AroundInterceptor;
import org.bithon.agent.bootstrap.aop.interceptor.IInterceptor;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Locale;


/**
 * @author frankchen
 * @date 2021-02-18 20:20
 */
public class AfterAdvice {
    public static final IAopLogger LOG = BootstrapHelper.createAopLogger(AfterAdvice.class);

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodEnter
    public static void onEnter(
        final @TargetMethod Method method,
        final @Advice.This(optional = true) Object target,
        @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
        @Advice.Local("context") Object context
    ) {
        AopContextImpl aopContext = new AopContextImpl(method, target, args);

        aopContext.onBeforeTargetMethodInvocation();

        // assign the context so that the leave method can access this object
        context = aopContext;
    }

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(final @Interceptor IInterceptor interceptor,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returning,
                              final @Advice.Thrown Throwable exception,
                              final @Advice.Local("context") Object context) {

        AopContextImpl aopContext = (AopContextImpl) context;
        aopContext.onAfterTargetMethodInvocation();
        aopContext.setException(exception);
        aopContext.setReturning(returning);

        if (interceptor == null) {
            return;
        }

        try {
            ((AroundInterceptor) interceptor).after(aopContext);
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Exception occurred when executing onExit of [%s] for [%s]: %s",
                                    interceptor.getClass().getSimpleName(),
                                    aopContext.getTargetClass().getSimpleName(),
                                    e.getMessage()),
                      e);
        }

        returning = aopContext.getReturning();
    }
}

