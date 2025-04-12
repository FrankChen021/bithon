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

package org.bithon.agent.plugin.thread.jdk9;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.thread.jdk.metrics.ForkJoinPoolMetrics;
import org.bithon.agent.plugin.thread.jdk.metrics.ThreadPoolMetricRegistry;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * {@link ForkJoinPool#ForkJoinPool(int, ForkJoinPool.ForkJoinWorkerThreadFactory, Thread.UncaughtExceptionHandler, boolean, int, int, int, Predicate, long, TimeUnit)} 
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:15 下午
 */
public class ForkJoinPool$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        IBithonObject forkJoinPool = (IBithonObject) aopContext.getTarget();
        if (forkJoinPool.getInjectedObject() == null) {
            // The hook is on all ctors,
            // However, one ctor might call another one, so we have to prevent duplicate initialization

            ThreadPoolMetricRegistry registry = ThreadPoolMetricRegistry.getInstance();
            if (registry != null) {
                ForkJoinPool pool = aopContext.getTargetAs();
                registry.addThreadPool(pool,
                                       pool.getClass().getName(),
                                       // It's ideal to get the workerNamePrefix from the ForkJoinPool object,
                                       // However, since it's a private field,
                                       // it's not able to get it without adding --add-exports directive after JDK 9
                                       // So, we have to use a hard-coded value here.
                                       // This causes two different ForkJoinPool records the metrics in one metric set
                                       "ForkJoinPool.worker",
                                       ForkJoinPoolMetrics::new);
            }

            forkJoinPool.setInjectedObject("ForkJoinPool.worker");
        }
    }
}
