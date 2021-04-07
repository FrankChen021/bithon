package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
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
