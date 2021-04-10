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

package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.utils.MiscUtils;
import com.sbss.bithon.agent.plugin.jdbc.druid.metric.DruidSqlMetricCollector;

import java.sql.Statement;

/**
 * @author frankchen
 */
public class DruidSqlInterceptor extends AbstractInterceptor {

    DruidSqlMetricCollector metricCollector;

    @Override
    public boolean initialize() throws Exception {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("sql-metrics(druid)", DruidSqlMetricCollector.class);
        return super.initialize();
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
        aopContext.setUserContext(MiscUtils.cleanupConnectionString(statement.getConnection()
                                                                             .getMetaData()
                                                                             .getURL()));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        String connectionString = aopContext.castUserContextAs();
        if (connectionString != null) {
            metricCollector.update(aopContext.getMethod().getName(),
                                   connectionString,
                                   aopContext,
                                   aopContext.getCostTime());
        }
    }
}
