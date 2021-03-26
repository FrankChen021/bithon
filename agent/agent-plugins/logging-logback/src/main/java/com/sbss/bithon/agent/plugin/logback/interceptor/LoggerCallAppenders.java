package com.sbss.bithon.agent.plugin.logback.interceptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class LoggerCallAppenders extends AbstractInterceptor {
    private LogMetricCollector counter;

    @Override
    public boolean initialize() {
        counter = MetricCollectorManager.getInstance().register("logback", new LogMetricCollector());
        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        ILoggingEvent iLoggingEvent = (ILoggingEvent) context.getArgs()[0];
        if (iLoggingEvent.getLevel().toInt() != Level.ERROR.toInt()) {
            return;
        }
        IThrowableProxy exception = iLoggingEvent.getThrowableProxy();
        if (null != exception) {
            counter.addException((String) InterceptorContext.get("uri"),
                                 exception);
            //this.addTrace(exception.getMessage());
        } else if (iLoggingEvent.getMessage() != null) {
            //this.addTrace(iLoggingEvent.getMessage());
        }
    }
}
