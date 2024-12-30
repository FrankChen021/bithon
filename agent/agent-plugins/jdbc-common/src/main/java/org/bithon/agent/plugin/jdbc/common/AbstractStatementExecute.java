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

package org.bithon.agent.plugin.jdbc.common;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.sql.SqlMetricRegistry;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.sql.Statement;

/**
 * @author frankchen
 */
public abstract class AbstractStatementExecute extends AroundInterceptor {

    private final SqlMetricRegistry metricRegistry = SqlMetricRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        Statement statement = (Statement) aopContext.getTarget();
        IBithonObject connection = (IBithonObject) statement.getConnection();
        ConnectionContext connectionContext = (ConnectionContext) connection.getInjectedObject();
        if (connectionContext == null) {
            // An unsupported PG driver
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceContextFactory.newSpan("postgresql");
        if (span != null) {
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Database.SYSTEM, "postgresql")
                .tag(Tags.Database.USER, connectionContext.getUserName())
                .tag(Tags.Database.CONNECTION_STRING, connectionContext.getConnectionString())
                .start();
        }

        aopContext.setUserContext(connectionContext);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext != null) {
            fillSpan(aopContext, traceContext.currentSpan());

            traceContext.currentSpan()
                        // the statement is injected in the ctor interceptor of PgPreparedStatement
                        .tag(Tags.Database.STATEMENT, getStatement(aopContext))
                        .finish();
        }

        ConnectionContext connectionContext = aopContext.getUserContext();
        metricRegistry.getOrCreateMetrics(connectionContext.getConnectionString())
                      .update(true, aopContext.hasException(), aopContext.getExecutionTime());
    }

    protected abstract String getStatement(AopContext aopContext);

    protected void fillSpan(AopContext aopContext, ITraceSpan span) {
    }
}
