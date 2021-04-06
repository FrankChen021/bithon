package com.sbss.bithon.agent.plugin.log4j2.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.plugin.log4j2.LogMetricCollector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;

/**
 * @author frankchen
 */
public class LoggerLogMessage extends AbstractInterceptor {
    private LogMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("log4j2", LogMetricCollector.class);
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Level logLevel = (Level) aopContext.getArgs()[1];
        Throwable e = (Throwable) aopContext.getArgs()[4];
        return e != null && StandardLevel.ERROR.equals(logLevel.getStandardLevel()) ?
               InterceptionDecision.CONTINUE : InterceptionDecision.SKIP_LEAVE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Throwable e = (Throwable) aopContext.getArgs()[4];
        metricCollector.addException((String) InterceptorContext.get("uri"),
                                     e);
    }
}
