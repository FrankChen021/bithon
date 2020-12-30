package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

import java.util.concurrent.ForkJoinPool;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:15 下午
 */
public class ForkJoinPoolExternalPush extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ForkJoinPool pool = aopContext.castTargetAs();

        //TODO: record task exception of ForkJoinPool
        ThreadPoolMetricsProvider.getInstance().addTotal(pool);
    }
}
