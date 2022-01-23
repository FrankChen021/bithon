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

package org.bithon.agent.core.metric.domain.thread;

import org.bithon.agent.core.metric.model.IMetricSet;
import org.bithon.agent.core.metric.model.IMetricValueProvider;
import org.bithon.agent.core.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
public class ThreadPoolMetrics implements IMetricSet {

    public Sum callerRunTaskCount = new Sum();
    public Sum abortedTaskCount = new Sum();
    public Sum discardedTaskCount = new Sum();
    public Sum discardedOldestTaskCount = new Sum();
    public Sum exceptionTaskCount = new Sum();
    public Sum successfulTaskCount = new Sum();
    public Sum totalTaskCount = new Sum();
    public final IMetricValueProvider activeThreads;
    public final IMetricValueProvider currentPoolSize;
    public final IMetricValueProvider maxPoolSize;
    public final IMetricValueProvider largestPoolSize;
    public final IMetricValueProvider queuedTaskCount;

    private final IMetricValueProvider[] metrics;

    public ThreadPoolMetrics(IMetricValueProvider activeThreads,
                             IMetricValueProvider currentPoolSize,
                             IMetricValueProvider maxPoolSize,
                             IMetricValueProvider largestPoolSize, IMetricValueProvider queuedTaskCount) {
        this.activeThreads = activeThreads;
        this.currentPoolSize = currentPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.largestPoolSize = largestPoolSize;
        this.queuedTaskCount = queuedTaskCount;

        this.metrics = new IMetricValueProvider[]{
            callerRunTaskCount,
            abortedTaskCount,
            discardedTaskCount,
            discardedOldestTaskCount,
            exceptionTaskCount,
            successfulTaskCount,
            totalTaskCount,
            activeThreads,
            currentPoolSize,
            maxPoolSize,
            largestPoolSize,
            queuedTaskCount
        };
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
