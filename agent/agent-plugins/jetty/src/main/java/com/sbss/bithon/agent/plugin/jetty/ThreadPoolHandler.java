package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class ThreadPoolHandler extends AbstractMethodIntercepted {

    private QueuedThreadPool threadPool;

    @Override
    public boolean init() throws Exception {
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (null == threadPool) {
            threadPool = (QueuedThreadPool) joinPoint.getTarget();
        }
    }

    QueuedThreadPool getThreadPool() {
        return threadPool;
    }

}
