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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.expt.AgentException;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolExecutorMetrics;
import org.bithon.agent.plugin.thread.metrics.ThreadPoolMetricRegistry;
import org.bithon.agent.plugin.thread.utils.ThreadPoolUtils;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
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
public class ThreadPoolExecutor$Ctor extends AbstractInterceptor {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ThreadPoolExecutor$Ctor.class);

    /**
     * Since defaultThreadFactory returns a new object for each call,
     * in order to reduce unnecessary object creation, we cache its class name
     */
    private static final String DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory().getClass().getName();

    @Override
    public void onConstruct(AopContext aopContext) {
        ThreadPoolMetricRegistry registry = ThreadPoolMetricRegistry.getInstance();
        if (registry == null) {
            return;
        }

        ThreadPoolExecutor executor = aopContext.castTargetAs();
        String poolName = detectThreadPoolName(executor);
        if (poolName != null) {
            registry.addThreadPool(executor, executor.getClass().getName(), poolName, new ThreadPoolExecutorMetrics(executor));
        }
    }

    private String detectThreadPoolName(ThreadPoolExecutor executor) {
        //
        // For default thread factory, it's pool name is meaningless
        // So, we find the caller name as the thread pool name,
        //
        ThreadFactory threadFactory = executor.getThreadFactory();
        if (DEFAULT_THREAD_FACTORY.equals(threadFactory.getClass().getName())) {
            StackTraceElement[] stackTraceElements = new RuntimeException().getStackTrace();

            // index 0 is current method, skip it
            for (int i = 1; i < stackTraceElements.length; i++) {
                StackTraceElement stack = stackTraceElements[i];
                if (!stack.getClassName().startsWith("java.util.concurrent")
                && !stack.getClassName().startsWith("org.bithon.agent.plugin.thread")) {
                    return stack.getClassName();
                }
            }
        }

        try {
            return ThreadPoolUtils.getThreadPoolName(threadFactory);
        } catch (AgentException e) {
            LOG.warn(e.getMessage());
            return null;
        }
    }
}
