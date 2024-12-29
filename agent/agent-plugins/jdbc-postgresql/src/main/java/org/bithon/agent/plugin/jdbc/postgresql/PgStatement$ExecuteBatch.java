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

package org.bithon.agent.plugin.jdbc.postgresql;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.utils.MiscUtils;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.sql.Statement;

/**
 * {@link org.postgresql.jdbc.PgStatement#executeBatch()}
 * {@link org.postgresql.jdbc.PgStatement#executeLargeBatch()}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 20:15
 */
public class PgStatement$ExecuteBatch extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        Statement statement = (Statement) aopContext.getTarget();

        String connectionString = MiscUtils.cleanupConnectionString(statement.getConnection()
                                                                             .getMetaData()
                                                                             .getURL());

        ITraceSpan span = TraceContextFactory.newSpan("mysql");
        if (span != null) {
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Database.SYSTEM, "mysql")
                .tag(Tags.Database.USER, statement.getConnection().getMetaData().getUserName())
                .tag(Tags.Database.CONNECTION_STRING, connectionString)
                .start();
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext != null) {
            traceContext.currentSpan()
                        .tag(Tags.Database.STATEMENT, aopContext.getArgs()[0])
                        .finish();
        }
    }
}
