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

package org.bithon.agent.plugin.alibaba.druid.interceptor;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.pool.DruidPooledStatement;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.sql.SqlMetricRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.alibaba.druid.ConnectionContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.sql.Statement;
import java.util.Locale;

/**
 * @author frankchen
 */
public abstract class DruidStatementAbstractExecute extends AroundInterceptor {
    static class UserContext {
        String uri;
        ITraceSpan span;

        public UserContext(String uri, ITraceSpan span) {
            this.uri = uri;
            this.span = span;
        }
    }

    private static final ILogAdaptor log = LoggerFactory.getLogger(DruidStatementAbstractExecute.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        Statement statement = aopContext.getTargetAs();

        DruidPooledConnection connection = (DruidPooledConnection) statement.getConnection();
        ConnectionContext connectionContext = ConnectionContext.from(connection);

        ITraceSpan span = TraceContextFactory.newSpan("alibaba-druid");
        if (span != null) {
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Database.SYSTEM, connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ENGLISH))
                .tag(Tags.Database.USER, connectionContext.getUsername())
                .tag(Tags.Database.CONNECTION_STRING, connectionContext.getConnectionString())
                .start();
        }

        aopContext.setUserContext(new UserContext(connectionContext.getConnectionString(), span));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        UserContext context = aopContext.getUserContext();
        if (context.span != null) {
            try {
                context.span.tag(Tags.Database.STATEMENT, getExecutingSql(aopContext))
                            .tag(aopContext.getException());

                if ("executeBatch".equals(aopContext.getMethod())) {
                    if (aopContext.getReturning() != null) {
                        context.span.tag(Tags.Database.PREFIX + "rows", Integer.toString(((int[]) aopContext.getReturning()).length));
                    }
                }
            } finally {
                context.span.finish();
            }
        }

        DruidPooledStatement druidStatement = aopContext.getTargetAs();
        boolean isInstrumented = druidStatement.getStatement() instanceof IBithonObject;

        if (context.uri != null
            // Record the metrics at the druid connection pool level
            // if the underlying JDBC has not been supported manually for metrics
            && !isInstrumented) {

            String methodName = aopContext.getMethod();

            // check if the metrics provider for this driver exists
            Boolean isQuery = null;
            if ("executeUpdate".equals(methodName)
                || "executeBatch".equals(methodName)) {
                isQuery = false;
            } else if ("execute".equals(methodName)) {
                /*
                 * execute method return true if the first result is a ResultSet
                 */
                isQuery = aopContext.getReturning() == null ? null : (boolean) aopContext.getReturningAs();
            } else if ("executeQuery".equals(methodName)) {
                isQuery = true;
            } else {
                //TODO: parse the SQL to check if it's a SELECT
                log.warn("unknown method intercepted by druid-sql-counter : {}", methodName);
            }

            SqlMetricRegistry.get()
                             .getOrCreateMetrics(context.uri).update(isQuery, aopContext.hasException(), aopContext.getExecutionTime());
        }
    }

    protected abstract String getExecutingSql(AopContext aopContext);
}
