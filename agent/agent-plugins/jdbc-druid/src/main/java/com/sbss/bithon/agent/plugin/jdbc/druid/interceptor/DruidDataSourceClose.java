package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.plugin.jdbc.druid.metric.MonitoredSourceManager;

/**
 * @author frankchen
 */
public class DruidDataSourceClose extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext context) {
        MonitoredSourceManager.getInstance().rmvDataSource(context.castTargetAs());

        return InterceptionDecision.SKIP_LEAVE;
    }
}
