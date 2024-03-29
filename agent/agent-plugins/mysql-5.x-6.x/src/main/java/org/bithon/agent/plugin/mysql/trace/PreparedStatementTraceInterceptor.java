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
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * @author frankchen
 */
public class PreparedStatementTraceInterceptor extends AroundInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(PreparedStatementTraceInterceptor.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("mysql");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a span and save it in user-context
        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .kind(SpanKind.CLIENT)
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        try {
            String sql;
            if ((sql = (String) InterceptorContext.get(ConnectionTraceInterceptor.KEY)) != null) {
                span.tag(Tags.Database.STATEMENT, sql);
            }
            span.finish();
        } finally {
            InterceptorContext.remove(ConnectionTraceInterceptor.KEY);
        }
    }
}
