package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.tomcat.metric.ExceptionMetricCollector;

/**
 * handle exception thrown by tomcat service, not by the tomcat itself
 *
 * @author frankchen
 */
public class StandardWrapperValveException extends AbstractInterceptor {

    private ExceptionMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("tomcat-exception", ExceptionMetricCollector.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        metricCollector.update((Throwable) context.getArgs()[2]);
    }
}
