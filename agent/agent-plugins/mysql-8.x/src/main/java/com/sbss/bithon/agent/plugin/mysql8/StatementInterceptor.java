package com.sbss.bithon.agent.plugin.mysql8;


import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class StatementInterceptor extends AbstractInterceptor {
    private SqlMetricProvider counter;

    @Override
    public boolean initialize() {
        counter = SqlMetricProvider.getInstance();
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        counter.update(context);
    }
}
