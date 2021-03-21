package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.core.utils.MiscUtils;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author frankchen
 */
public class PreparedStatementInterceptor extends AbstractInterceptor {
    private SqlMetricCollector counter;
    private StatementCounterStorage sqlCounter;

    @Override
    public boolean initialize() {
        counter = SqlMetricCollector.getInstance();
        sqlCounter = StatementCounterStorage.getInstance();
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
        counter.recordExecution(aopContext, (String) aopContext.getUserContext());
        sqlCounter.sqlStats(aopContext, (String) aopContext.getUserContext());
    }
}
