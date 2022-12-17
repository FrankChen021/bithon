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
import org.bithon.shaded.net.bytebuddy.asm.Advice;

import java.lang.reflect.Constructor;
import java.util.Locale;


/**
 * @author frankchen
 * @date 2021-02-18 18:03
 */
public class ConstructorDecoratorAdvice {
    public static final IAopLogger LOG = BootstrapHelper.createAopLogger(ConstructorDecoratorAdvice.class);

    @Advice.OnMethodExit
    public static void onExit(final @Interceptor AbstractInterceptor interceptor,
                              final @TargetMethod Constructor<?> method,
                              final @Advice.This Object target,
                              final @Advice.AllArguments Object[] args) {
        if (interceptor == null) {
            return;
        }
        try {
            interceptor.onConstruct(new AopContext(method, target, args));
        } catch (Exception e) {
            LOG.error(String.format(Locale.ENGLISH,
                                    "Exception occurs when executing onConstruct on interceptor [%s]: %s",
                                    interceptor.getClass().getName(),
                                    e.getMessage()),
                      e);
        }
    }
}
