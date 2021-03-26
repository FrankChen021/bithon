package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
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
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("sql-metrics(druid)", DruidSqlMetricCollector.class);
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
