package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:08 下午
 */
public class ThreadPoolExecutorAfterExecute extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext joinPoint) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) joinPoint.getTarget();
        Throwable exception = (Throwable) joinPoint.getArgs()[1];
        ThreadPoolMetricsProvider.getInstance().addRunCount(executor, exception != null);
    }
}
