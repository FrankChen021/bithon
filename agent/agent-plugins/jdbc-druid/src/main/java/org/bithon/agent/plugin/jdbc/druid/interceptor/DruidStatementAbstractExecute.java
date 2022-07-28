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

package org.bithon.agent.plugin.jdbc.druid.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.metric.domain.sql.SqlMetricRegistry;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import org.bithon.agent.core.utils.MiscUtils;
import org.bithon.agent.plugin.jdbc.druid.DruidPlugin;
import org.bithon.agent.plugin.jdbc.druid.config.DruidPluginConfig;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.sql.Statement;

/**
 * @author frankchen
 */
public abstract class DruidStatementAbstractExecute extends AbstractInterceptor {
    static class UserContext {
        String uri;
        ITraceSpan span;

        public UserContext(String uri, ITraceSpan span) {
            this.uri = uri;
            this.span = span;
        }
    }

    private static final ILogAdaptor log = LoggerFactory.getLogger(DruidStatementAbstractExecute.class);

    private SqlMetricRegistry metricRegistry;
    private boolean isSQLMetricEnabled;

    @Override
    public boolean initialize() {
        DruidPluginConfig config = AgentContext.getInstance().getAgentConfiguration().getConfig(DruidPluginConfig.class);
        isSQLMetricEnabled = config.isSQLMetricEnabled();
        if (isSQLMetricEnabled) {
            metricRegistry = SqlMetricRegistry.get();
        }
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Statement statement = aopContext.castTargetAs();

        // TODO: cache the cleaned-up connection string in IBithonObject after connection object instantiation
        // to improve performance
        //
        // Get connection string before a SQL execution
        // In some cases, a connection might be aborted by server
        // then, a getConnection() call would throw an exception saying that connection has been closed
        String connectionString = MiscUtils.cleanupConnectionString(statement.getConnection()
                                                                             .getMetaData()
                                                                             .getURL());
        ITraceSpan span = TraceSpanFactory.newSpan("alibaba-druid");
        if (span != null) {
            span.method(aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag("db", connectionString)
                .start();
        }

        aopContext.setUserContext(new UserContext(connectionString, span));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        UserContext context = aopContext.castUserContextAs();
        if (context.span != null) {
            try {
                context.span.tag(Tags.SQL, getExecutingSql(aopContext))
                            .tag(aopContext.getException());

                if (DruidPlugin.METHOD_EXECUTE_BATCH.equals(aopContext.getMethod().getName())) {
                    if (aopContext.getReturning() != null) {
                        context.span.tag("rows", Integer.toString(((int[]) aopContext.getReturning()).length));
                    }
                }
            } finally {
                context.span.finish();
            }
        }

        if (context.uri != null && isSQLMetricEnabled) {

            String methodName = aopContext.getMethod().getName();

            // check if metrics provider for this driver exists
            Boolean isQuery = null;
            if (DruidPlugin.METHOD_EXECUTE_UPDATE.equals(methodName)
                || DruidPlugin.METHOD_EXECUTE_BATCH.equals(methodName)) {
                isQuery = false;
            } else if (DruidPlugin.METHOD_EXECUTE.equals(methodName)) {
                /*
                 * execute method return true if the first result is a ResultSet
                 */
                isQuery = aopContext.getReturning() == null ? null : (boolean) aopContext.castReturningAs();
            } else if (DruidPlugin.METHOD_EXECUTE_QUERY.equals(methodName)) {
                isQuery = true;
            } else {
                //TODO: parse the SQL to check if it's a SELECT
                log.warn("unknown method intercepted by druid-sql-counter : {}", methodName);
            }

            metricRegistry.getOrCreateMetrics(context.uri).update(isQuery, aopContext.hasException(), aopContext.getCostTime());
        }
    }

    protected abstract String getExecutingSql(AopContext aopContext);
}
