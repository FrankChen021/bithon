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

package org.bithon.agent.plugin.mysql.trace;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.context.InterceptorContext;

/**
 * @author frankchen
 */
public class ConnectionTraceInterceptor extends AroundInterceptor {

    public static final String KEY = "sql";

    @Override
    public InterceptionDecision before(AopContext context) {
        if (context.getArgs() != null && context.getArgs().length > 0) {
            InterceptorContext.set(KEY, context.getArgs()[0].toString());
            return InterceptionDecision.CONTINUE;
        }

        return InterceptionDecision.SKIP_LEAVE;
    }

    @Override
    public void after(AopContext aopContext) {
        //InterceptorContext.remove(KEY);
    }
}
