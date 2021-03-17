package com.sbss.bithon.agent.plugin.log4j2;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;

/**
 * @author frankchen
 */
public class LogMessageInterceptor extends AbstractInterceptor {
    private LogMetricProvider counter;

    @Override
    public boolean initialize() {
        counter = (LogMetricProvider) MetricProviderManager.getInstance().register("log4j2", new LogMetricProvider());
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
        counter.addException((String) InterceptorContext.get("uri"),
                             e);
    }
}
