package com.sbss.bithon.agent.plugin.mysql8;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class PreparedStatementInterceptor extends AbstractInterceptor {
    private SqlMetricCollector sqlCounter;
    private SqlStatementMetricCollector statementCounterStorage;

    @Override
    public boolean initialize() {
        sqlCounter = SqlMetricCollector.getInstance();
        statementCounterStorage = SqlStatementMetricCollector.getInstance();
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        sqlCounter.update(context);
        statementCounterStorage.update(context);
    }
}
