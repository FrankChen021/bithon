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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.BootstrapHelper;
import org.bithon.agent.bootstrap.aop.IAopLogger;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Locale;


/**
 * @author frankchen
 * @date 2021-02-18 20:20
 */
public class MethodDecoratorAdvice {
    public static final IAopLogger LOG = BootstrapHelper.createAopLogger(MethodDecoratorAdvice.class);

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodEnter
    public static boolean onEnter(
        final @Interceptor AbstractInterceptor interceptor,
        final @TargetMethod Method method,
        final @Advice.This(optional = true) Object target,
        @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
        @Advice.Local("context") Object context
    ) {
        if (interceptor == null) {
            return false;
        }

        AopContext aopContext = new AopContext(method, target, args);
        context = aopContext;

        boolean skipLeaveMethod = true;
        try {
            skipLeaveMethod = interceptor.onMethodEnter(aopContext) == InterceptionDecision.SKIP_LEAVE;
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH, "Exception occurred when executing onEnter of [%s] for [%s]: %s",
                                    interceptor.getClass().getSimpleName(),
                                    method.getDeclaringClass().getSimpleName(),
                                    e.getMessage()),
                      e);

            // continue to execute
        }

        //this assignment must be kept since it tells bytebuddy that args might have been re-written
        // so that bytebyddy re-map the args to original function input argument
        args = aopContext.getArgs();

        if (skipLeaveMethod) {
            return false;
        }
        aopContext.onBeforeTargetMethodInvocation();

        return true;
    }

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(final @Interceptor AbstractInterceptor interceptor,
                              final @Advice.Enter boolean shouldExecute,
                              @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returning,
                              final @Advice.Thrown Throwable exception,
                              final @Advice.Local("context") Object context) {
        if (!shouldExecute || context == null) {
            return;
        }

        AopContext aopContext = (AopContext) context;
        aopContext.onAfterTargetMethodInvocation();
        aopContext.setException(exception);
        aopContext.setReturning(returning);

        if (interceptor == null) {
            return;
        }

        try {
            interceptor.onMethodLeave(aopContext);
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

