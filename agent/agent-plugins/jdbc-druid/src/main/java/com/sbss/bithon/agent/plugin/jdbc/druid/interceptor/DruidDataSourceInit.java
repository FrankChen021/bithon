package com.sbss.bithon.agent.plugin.jdbc.druid.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.plugin.jdbc.druid.metric.DruidJdbcMetricCollector;
import com.sbss.bithon.agent.plugin.jdbc.druid.metric.MonitoredSourceManager;

/**
 * @author frankchen
 */
public class DruidDataSourceInit extends AbstractInterceptor {

    @Override
    public boolean initialize() throws Exception {
        DruidJdbcMetricCollector.getOrCreateInstance();
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        IBithonObject obj = aopContext.castTargetAs();
        Boolean initialized = (Boolean) obj.getInjectedObject();
        if (initialized != null && initialized) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        if (aopContext.hasException()) {
            return;
        }

        IBithonObject obj = aopContext.castTargetAs();
        boolean initialized = MonitoredSourceManager.getInstance().addDataSource(aopContext.castTargetAs());
        obj.setInjectedObject(initialized);
    }
}
