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

package org.bithon.agent.plugin.mysql.metrics;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.sql.SqlMetricCollector;
import org.bithon.agent.core.utils.MiscUtils;
import org.bithon.agent.plugin.mysql.MySqlPlugin;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author frankchen
 */
public class PreparedStatementInterceptor extends AbstractInterceptor {
    private SqlMetricCollector sqlMetricCollector;
    private StatementMetricCollector statementMetricCollector;

    @Override
    public boolean initialize() {
        sqlMetricCollector = MetricCollectorManager.getInstance()
                                                   .getOrRegister("mysql-metrics", SqlMetricCollector.class);
        statementMetricCollector = StatementMetricCollector.getInstance();
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        try {
            Statement statement = (Statement) aopContext.getTarget();
            String connectionString = statement.getConnection().getMetaData().getURL();

            aopContext.setUserContext(MiscUtils.cleanupConnectionString(connectionString));
        } catch (SQLException ignored) {
            return InterceptionDecision.SKIP_LEAVE;
        }
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        String connectionString = aopContext.castUserContextAs();
        if (connectionString == null) {
            return;
        }

        String methodName = aopContext.getMethod().getName();
        boolean isQuery = true;
        if (MySqlPlugin.METHOD_EXECUTE_UPDATE.equals(methodName) ||
            MySqlPlugin.METHOD_EXECUTE_UPDATE_INTERNAL.equals(methodName)) {
            isQuery = false;
        } else if ((MySqlPlugin.METHOD_EXECUTE.equals(methodName) ||
                    MySqlPlugin.METHOD_EXECUTE_INTERNAL.equals(methodName))) {
            Object result = aopContext.castReturningAs();
            if (result instanceof Boolean && !(boolean) result) {
                isQuery = false;
            }
        }
        sqlMetricCollector.getOrCreateMetric(connectionString)
                          .update(isQuery, aopContext.hasException(), aopContext.getCostTime());


        statementMetricCollector.sqlStats(aopContext, (String) aopContext.getUserContext());
    }
}
