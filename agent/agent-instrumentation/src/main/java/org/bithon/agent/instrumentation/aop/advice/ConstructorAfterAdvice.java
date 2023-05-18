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

import java.util.Locale;


/**
 * @author frankchen
 * @date 2021-02-18 18:03
 */
public class ConstructorAfterAdvice {
    public static final ILogger LOG = LoggerFactory.getLogger(ConstructorAfterAdvice.class);

    @Advice.OnMethodExit
    public static void onExit(@AdviceAnnotation.InterceptorName String name,
                              @AdviceAnnotation.InterceptorIndex int index,
                              @Advice.Origin Class<?> clazz,
                              @Advice.Origin("#m") String method,
                              @Advice.This Object target,
                              @Advice.AllArguments Object[] args) {
        AbstractInterceptor interceptor = InterceptorManager.INSTANCE.getInterceptor(index);
        if (interceptor == null) {
            return;
        }
        interceptor.hit();

        try {
            ((AfterInterceptor) interceptor).after(new AopContextImpl(clazz, method, target, args));
        } catch (Throwable e) {
            LOG.error(String.format(Locale.ENGLISH,
                                    "Exception occurs when executing onConstruct on interceptor [%s]: %s",
                                    name,
                                    e.getMessage()),
                      e);
        }
    }
}
