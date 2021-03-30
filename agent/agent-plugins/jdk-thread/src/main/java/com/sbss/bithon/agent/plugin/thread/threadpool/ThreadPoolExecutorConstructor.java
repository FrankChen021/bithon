package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:10 下午
 */
public class ThreadPoolExecutorConstructor extends AbstractInterceptor {
    @Override
    public void onConstruct(AopContext aopContext) {
        ThreadPoolMetricsCollector collector = ThreadPoolMetricsCollector.getInstance();
        if (collector != null) {
            ThreadPoolExecutor executor = aopContext.castTargetAs();
            collector.addThreadPool(executor, new ThreadPoolExecutorCompositeMetric(executor));
        }
    }
}
