package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author frankchen
 */
public class ThreadPoolHandler extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext context) {
        WebServerMetricProvider.getInstance().setThreadPool((QueuedThreadPool) context.getTarget());
    }
}
