package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.jdbc.druid.metric.DruidJdbcMetricProvider;

/**
 * @author frankchen
 */
public class DruidDataSourceGetValueAndReset extends AbstractInterceptor {
    @Override
    public void onMethodLeave(AopContext aopContext) {
        DruidJdbcMetricProvider.getOrCreateInstance().updateMetrics(aopContext.castTargetAs(),
                                                                    aopContext.castReturningAs());
    }
}
