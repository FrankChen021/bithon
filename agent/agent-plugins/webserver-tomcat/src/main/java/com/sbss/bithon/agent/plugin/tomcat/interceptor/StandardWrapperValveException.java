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

    private ExceptionMetricCollector metricProvider;

    @Override
    public boolean initialize() {
        metricProvider = MetricCollectorManager.getInstance()
                                               .register("tomcat-exception", new ExceptionMetricCollector());
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        metricProvider.update((Throwable) context.getArgs()[2]);
    }
}
