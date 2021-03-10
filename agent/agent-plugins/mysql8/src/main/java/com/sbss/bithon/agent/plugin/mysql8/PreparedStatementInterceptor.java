package com.sbss.bithon.agent.plugin.mysql8;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class PreparedStatementInterceptor extends AbstractInterceptor {
    private SqlMetricProvider sqlCounter;
    private SqlStatementMetricProvider statementCounterStorage;

    @Override
    public boolean initialize() {
        sqlCounter = SqlMetricProvider.getInstance();
        statementCounterStorage = SqlStatementMetricProvider.getInstance();
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        sqlCounter.update(context);
        statementCounterStorage.update(context);
    }
}
