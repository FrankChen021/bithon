package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;

import java.util.concurrent.ForkJoinPool;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:15 下午
 */
public class ForkJoinPoolConstructor extends AbstractInterceptor {

    @Override
    public void onConstruct(Object constructedObject, Object[] args) {
        ForkJoinPool pool = (ForkJoinPool) constructedObject;
        ThreadPoolMetricsProvider.getInstance().insertThreadPoolMetrics(pool, new ForkJoinPoolMetrics(pool));
    }
}
