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
import org.bithon.agent.observability.metric.domain.sql.SQLMetricStorage;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author frankchen
 */
public abstract class AbstractStatement$Execute extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        Statement statement = (Statement) aopContext.getTarget();

        Connection connection = statement.getConnection();
        ConnectionContext connectionContext = getConnectionContext(connection);
        if (connectionContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceSpan span = TraceContextFactory.newSpan(connectionContext.getDbType());
        if (span != null) {
            aopContext.setSpan(span);
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Database.SYSTEM, connectionContext.getDbType())
                .tag(Tags.Database.USER, connectionContext.getUserName())
                .tag(Tags.Database.CONNECTION_STRING, connectionContext.getConnectionString())
                .start();
        }

        aopContext.setUserContext(connectionContext);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        StatementContext statement = getStatementContext(aopContext);

        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            fillSpan(aopContext, span);

            // the statement is injected in the ctor interceptor of PgPreparedStatement
            span.tag(Tags.Database.STATEMENT, statement.getSql())
                .tag(Tags.Database.OPERATION, statement.getSqlType())
                .finish();
        }

        if (shouldRecordMetrics(aopContext.getTargetAs())) {
            ConnectionContext connectionContext = aopContext.getUserContext();

            SQLMetricStorage.getInstance()
                            .add(Dimensions.of(connectionContext.getConnectionString(),
                                               statement.getSqlType(),
                                               span != null ? span.traceId() : "",
                                               statement.getSql()),
                                 (metrics) -> {
                                     metrics.callCount = 1;
                                     metrics.responseTime = aopContext.getExecutionTime();
                                     metrics.maxResponseTime = aopContext.getExecutionTime();
                                     metrics.minResponseTime = aopContext.getExecutionTime();
                                     metrics.bytesOut = statement.getSql().length();
                                     metrics.errorCount = aopContext.hasException() ? 1 : 0;
                                 });
        }
    }

    /**
     * Get the connection context which is injected by interceptors on Connection objects
     */
    protected ConnectionContext getConnectionContext(Connection connection) throws SQLException {
        if (!(connection instanceof IBithonObject)) {
            return null;
        }

        return (ConnectionContext) ((IBithonObject) connection).getInjectedObject();
    }

    protected abstract StatementContext getStatementContext(AopContext aopContext);

    protected void fillSpan(AopContext aopContext, ITraceSpan span) {
    }

    protected boolean shouldRecordMetrics(Statement statement) {
        return true;
    }
}
