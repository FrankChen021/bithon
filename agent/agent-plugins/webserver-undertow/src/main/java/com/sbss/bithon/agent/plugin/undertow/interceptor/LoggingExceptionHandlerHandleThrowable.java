package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.undertow.metric.ExceptionMetricProvider;

/**
 * @author frankchen
 */
public class LoggingExceptionHandlerHandleThrowable extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        ExceptionMetricProvider.getInstance().update((Throwable) context.getArgs()[3]);
    }
}
