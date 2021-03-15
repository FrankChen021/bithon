package com.sbss.bithon.agent.plugin.mysql8;


import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class IoInterceptor extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        SqlMetricProvider.getInstance().update(context);
    }
}
