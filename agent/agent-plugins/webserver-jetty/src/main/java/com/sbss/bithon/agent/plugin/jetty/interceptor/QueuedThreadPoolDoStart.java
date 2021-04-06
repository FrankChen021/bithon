package com.sbss.bithon.agent.plugin.jetty.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.plugin.jetty.metric.WebServerMetricCollector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author frankchen
 */
public class QueuedThreadPoolDoStart extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        WebServerMetricCollector.getInstance().setThreadPool((QueuedThreadPool) context.getTarget());
    }
}
