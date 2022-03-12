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
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * @author Frank Chen
 * @date 22/2/22 11:25 PM
 */
public class MethodReplacementAdvice {
    /**
     * This method is only used for bytebuddy method advice. Have no use during the execution since the code has been injected into target class
     */
    @Advice.OnMethodExit
    public static void onExecute(final @Interceptor AbstractInterceptor interceptor,
                                 final @Advice.AllArguments Object[] args,
                                 @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returning) {
        if (interceptor != null) {
            returning = interceptor.onExecute(args);
        }
    }
}
