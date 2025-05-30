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

package org.bithon.agent.plugin.thread.jdk.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.plugin.thread.jdk.metrics.ThreadPoolExecutorMetrics;
import org.bithon.agent.plugin.thread.jdk.metrics.ThreadPoolMetricRegistry;
import org.bithon.agent.plugin.thread.jdk.utils.ObservedExecutionHandler;
import org.bithon.agent.plugin.thread.jdk.utils.ThreadPoolNameExtractor;
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
public class ThreadPoolExecutor$Ctor extends AroundInterceptor {

    public InterceptionDecision before(AopContext aopContext) {
        RejectedExecutionHandler handler = aopContext.getArgAs(6);
        if (handler != null) {
            aopContext.getArgs()[6] = new ObservedExecutionHandler(handler);
        }
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ThreadPoolMetricRegistry registry = ThreadPoolMetricRegistry.getInstance();
        if (registry == null) {
            return;
        }

        ThreadPoolExecutor executor = aopContext.getTargetAs();
        try {
            String poolName = ThreadPoolNameExtractor.INSTANCE.extract(executor);
            registry.addThreadPool(executor,
                                   executor.getClass().getName(),
                                   poolName,
                                   ThreadPoolExecutorMetrics::new);

            // Keep the pool name so that it can be used in ThreadPoolExecutor$Execute to be recorded in spans
            ((IBithonObject) aopContext.getTarget()).setInjectedObject(poolName);
        } catch (AgentException e) {
            LoggerFactory.getLogger(ThreadPoolExecutor$Ctor.class).warn(e.getMessage());
        }
    }
}
