package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:12 下午
 */
public class ThreadPoolExecutorShutdown extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext joinPoint) {
        ThreadPoolMetricsProvider.getInstance().deleteThreadPoolMetrics((ThreadPoolExecutor) joinPoint.getTarget());
    }
}
