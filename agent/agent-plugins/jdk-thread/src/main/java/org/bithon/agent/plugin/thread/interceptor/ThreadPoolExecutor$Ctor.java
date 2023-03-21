/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.plugin.thread.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolExecutorMetrics;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolMetricRegistry;
import org.bithon.agent.plugin.thread.utils.ThreadPoolNameHelper;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@link java.util.concurrent.ThreadPoolExecutor#ThreadPoolExecutor(int, int, long, TimeUnit, BlockingQueue, ThreadFactory, RejectedExecutionHandler)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:10 下午
 */
public class ThreadPoolExecutor$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        ThreadPoolMetricRegistry registry = ThreadPoolMetricRegistry.getInstance();
        if (registry == null) {
            return;
        }

        ThreadPoolExecutor executor = aopContext.getTargetAs();
        try {
            String poolName = ThreadPoolNameHelper.INSTANCE.getThreadPoolName(executor);
            registry.addThreadPool(executor,
                                   executor.getClass().getName(),
                                   poolName,
                                   ThreadPoolExecutorMetrics::new);
        } catch (AgentException e) {
            LoggerFactory.getLogger(ThreadPoolExecutor$Ctor.class).warn(e.getMessage());
        }
    }
}
