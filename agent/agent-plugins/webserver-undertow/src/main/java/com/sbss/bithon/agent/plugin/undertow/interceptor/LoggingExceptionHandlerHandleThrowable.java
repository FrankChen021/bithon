package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
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
