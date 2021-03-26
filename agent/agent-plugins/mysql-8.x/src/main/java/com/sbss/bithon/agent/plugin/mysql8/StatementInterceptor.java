package com.sbss.bithon.agent.plugin.mysql8;


import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.utils.MiscUtils;

import java.sql.Statement;

/**
 * @author frankchen
 */
public class StatementInterceptor extends AbstractInterceptor {
    private SqlMetricCollector sqlMetricCollector;

    @Override
    public boolean initialize() throws Exception {
        sqlMetricCollector = MetricCollectorManager.getInstance().getOrRegister("mysql8-metrics", SqlMetricCollector.class);

        return super.initialize();
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        Statement statement = (Statement) aopContext.getTarget();
        String connectionString = MiscUtils.cleanupConnectionString(statement.getConnection()
                .getMetaData()
                .getURL());

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
