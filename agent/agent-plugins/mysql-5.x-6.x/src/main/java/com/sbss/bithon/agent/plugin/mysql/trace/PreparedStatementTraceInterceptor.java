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

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class PreparedStatementTraceInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PreparedStatementTraceInterceptor.class);

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        TraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        aopContext.setUserContext(parentSpan.newChildSpan("mysql")
                                            .method(aopContext.getMethod())
                                            .kind(SpanKind.CLIENT)
                                            .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        TraceSpan mysqlSpan = aopContext.castUserContextAs();
        if (mysqlSpan == null) {
            return;
        }
        try {
            String sql;
            if ((sql = (String) InterceptorContext.get(ConnectionTraceInterceptor.KEY)) != null) {
                mysqlSpan.tag("sql", sql);
            }
        } finally {
            try {
                mysqlSpan.finish();
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }
}
