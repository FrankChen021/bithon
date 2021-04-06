package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
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
