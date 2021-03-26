package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.core.utils.MiscUtils;
import com.sbss.bithon.agent.plugin.mysql.MySqlPlugin;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author frankchen
 */
public class StatementInterceptor extends AbstractInterceptor {
    private SqlMetricCollector sqlMetricCollector;

    @Override
    public boolean initialize() {
        sqlMetricCollector = MetricCollectorManager.getInstance().getOrRegister("mysql-metrics", SqlMetricCollector.class);
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        try {
            Statement statement = (Statement) aopContext.getTarget();
            String connectionString = MiscUtils.cleanupConnectionString(statement.getConnection()
                    .getMetaData()
                    .getURL());
            aopContext.setUserContext(connectionString);
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

        boolean isQuery = true;
        String methodName = aopContext.getMethod().getName();
        if (MySqlPlugin.METHOD_EXECUTE_UPDATE.equals(methodName) || MySqlPlugin.METHOD_EXECUTE_UPDATE_INTERNAL.equals(methodName)) {
            isQuery = false;
        } else if ((MySqlPlugin.METHOD_EXECUTE.equals(methodName) || MySqlPlugin.METHOD_EXECUTE_INTERNAL.equals(methodName))) {
            Object result = aopContext.castReturningAs();
            if (result instanceof Boolean && !(boolean) result) {
                isQuery = false;
            }
        }

        sqlMetricCollector.getOrCreateMetric(connectionString).update(isQuery, aopContext.hasException(), aopContext.getCostTime());
    }
}
