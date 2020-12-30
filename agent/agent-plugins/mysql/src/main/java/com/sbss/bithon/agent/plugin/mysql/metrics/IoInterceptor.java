package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * MySQL IO
 *
 * @author frankchen
 */
public class IoInterceptor extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        SqlMetricProvider.getInstance().recordIO(context);
    }
}
