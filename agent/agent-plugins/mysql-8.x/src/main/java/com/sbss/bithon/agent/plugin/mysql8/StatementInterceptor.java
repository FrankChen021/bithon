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

package com.sbss.bithon.agent.plugin.mysql8;


import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlMetricCollector;
import com.sbss.bithon.agent.core.utils.MiscUtils;

import java.sql.Statement;

/**
 * @author frankchen
 */
public class StatementInterceptor extends AbstractInterceptor {
    private SqlMetricCollector sqlMetricCollector;

    @Override
    public boolean initialize() throws Exception {
        sqlMetricCollector = MetricCollectorManager.getInstance()
                                                   .getOrRegister("mysql8-metrics", SqlMetricCollector.class);

        return super.initialize();
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Statement statement = (Statement) aopContext.getTarget();

        // get the connection info before execution since the connection might be closed during execution
        aopContext.setUserContext(MiscUtils.cleanupConnectionString(statement.getConnection()
                                                                             .getMetaData()
                                                                             .getURL()));
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        String connectionString = aopContext.castUserContextAs();

        SqlCompositeMetric metric = sqlMetricCollector.getOrCreateMetric(connectionString);
        boolean isQuery = true;
        String methodName = aopContext.getMethod().getName();
        if (MySql8Plugin.METHOD_EXECUTE_UPDATE.equals(methodName)
            || MySql8Plugin.METHOD_EXECUTE_UPDATE_INTERNAL.equals(methodName)) {
            isQuery = false;
        } else if ((MySql8Plugin.METHOD_EXECUTE.equals(methodName) || MySql8Plugin.METHOD_EXECUTE_INTERNAL.equals(
            methodName))) {
            Object result = aopContext.castReturningAs();
            if (result instanceof Boolean && !(boolean) result) {
                isQuery = false;
            }
        }
        metric.update(isQuery, aopContext.hasException(), aopContext.getCostTime());
    }
}
