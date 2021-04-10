/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.mysql.trace;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;

/**
 * @author frankchen
 */
public class ConnectionTraceInterceptor extends AbstractInterceptor {

    public static final String KEY = "sql";

    @Override
    public InterceptionDecision onMethodEnter(AopContext context) {
        if (context.getArgs() != null && context.getArgs().length > 0) {
            InterceptorContext.set(KEY, context.getArgs()[0].toString());
            return InterceptionDecision.CONTINUE;
        }

        return InterceptionDecision.SKIP_LEAVE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        //InterceptorContext.remove(KEY);
    }
}
