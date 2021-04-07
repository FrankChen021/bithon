package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.plugin.undertow.metric.ExceptionMetricCollector;

/**
 * @author frankchen
 */
public class LoggingExceptionHandlerHandleThrowable extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        ExceptionMetricCollector.getInstance().update((Throwable) context.getArgs()[3]);
    }
}
