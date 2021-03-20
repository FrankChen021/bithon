package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.jdbc.druid.metric.MonitoredSourceManager;

/**
 * @author frankchen
 */
public class DruidDataSourceRestart extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        MonitoredSourceManager.getInstance().addDataSource(aopContext.castTargetAs());
    }
}
