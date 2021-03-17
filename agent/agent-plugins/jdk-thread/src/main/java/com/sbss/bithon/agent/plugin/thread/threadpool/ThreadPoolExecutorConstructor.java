package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:10 下午
 */
public class ThreadPoolExecutorConstructor extends AbstractInterceptor {
    @Override
    public void onConstruct(Object constructedObject,
                            Object[] args) throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) constructedObject;
        ThreadPoolMetricsProvider.getInstance()
                                 .insertThreadPoolMetrics(executor, new ThreadPoolExecutorMetrics(executor));
    }
}
