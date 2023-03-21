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
import org.bithon.agent.instrumentation.aop.interceptor.BeforeInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.IInterceptor;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Method;
import java.util.Locale;


/**
 * @author frankchen
 * @date 2021-02-18 20:20
 */
public class BeforeAdvice {
    public static final ILogger LOG = LoggerFactory.createLogger(BeforeAdvice.class);

    /**
     * this method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodEnter
    public static void onEnter(
        @AdviceAnnotation.Interceptor IInterceptor interceptor,
        @AdviceAnnotation.TargetMethod Method method,
        @Advice.This(optional = true) Object target,
        @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args
    ) {
        if (interceptor == null) {
            return;
        }

        AopContextImpl aopContext = new AopContextImpl(method, target, args);

        try {
            ((BeforeInterceptor) interceptor).before(aopContext);
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
    }
}

