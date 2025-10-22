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


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bithon.agent.instrumentation.aop.context.AopContextImpl;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AbstractInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * @author frank.chen021@outlook.com
 * @date 21/10/25 9:48 pm
 */
public class BeforeConstructorAdvice {

    /**
     * This method is only used for byte-buddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
    @Advice.OnMethodEnter
    public static void onEnter(@AdviceAnnotation.InterceptorName String name,
                               @AdviceAnnotation.InterceptorIndex int index,
                               @Advice.Origin Class<?> clazz,
                               @Advice.Origin("#m") String method,
                               @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args
    ) {
        AbstractInterceptor interceptor = InterceptorManager.INSTANCE.getInterceptor(index);
        if (interceptor == null) {
            return;
        }
        interceptor.hit();

        AopContextImpl aopContext = new AopContextImpl(clazz, method, null, args);
        try {
            ((BeforeInterceptor) interceptor).before(aopContext);
        } catch (Throwable e) {
            interceptor.onBeforeException(e);

            // continue to execute
        }

        // This assignment must be kept since it tells byte-buddy that args might have been re-written
        // so that byte-buddy re-map the args to original function input argument
        args = aopContext.getArgs();
    }
}
