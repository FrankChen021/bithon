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

package org.bithon.agent.observability.metric.domain.thread;

import org.bithon.agent.observability.metric.model.IMetricSet;
import org.bithon.agent.observability.metric.model.IMetricValueProvider;
import org.bithon.agent.observability.metric.model.Min;
import org.bithon.agent.observability.metric.model.Sum;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
public class ThreadPoolMetrics<T> implements IMetricSet {

    /**
     * rejectedTask = callRun + aborted + discarded + discardedOldest + user
     */
    public final Sum totalRejectedCount = new Sum();
    public final Sum callerRunTaskCount = new Sum();
    public final Sum abortedTaskCount = new Sum();
    public final Sum discardedTaskCount = new Sum();
    public final Sum discardedOldestTaskCount = new Sum();
    public final Sum userRejectedPolicyCount = new Sum();

    public final Sum exceptionTaskCount = new Sum();
    public final Sum successfulTaskCount = new Sum();
    public final Sum totalTaskCount = new Sum();
    public final Min minDuration = new Min();
    public final Sum duration = new Sum();
    public final Sum maxDuration = new Sum();
    public final IMetricValueProvider activeThreads;
    public final IMetricValueProvider currentPoolSize;
    public final IMetricValueProvider maxPoolSize;
    public final IMetricValueProvider largestPoolSize;
    public final IMetricValueProvider queuedTaskCount;

    /**
     * How many pool instances of the same pool name
     */
    public IMetricValueProvider poolCount;

    protected IMetricValueProvider[] metrics;

    private final List<T> executors = new ArrayList<>();

    protected long sum(Function<T, Number> valueSupplier) {
        long value = 0;
        synchronized (executors) {
            for (T executor : executors) {
                value += valueSupplier.apply(executor).longValue();
            }
        }
        return value;
    }

    public void add(T executor) {
        synchronized (this.executors) {
            this.executors.add(executor);
        }
    }

    public boolean remove(T executor) {
        synchronized (this.executors) {
            this.executors.remove(executor);
            return executors.isEmpty();
        }
    }

    public ThreadPoolMetrics(Function<T, Number> activeThreadsPerInstance,
                             Function<T, Number> currentPoolSizePerInstance,
                             Function<T, Number> maxPoolSizePerInstance,
                             Function<T, Number> largestPoolSizePerInstance,
                             Function<T, Number> queuedTaskCountPerInstance) {
        this.activeThreads = () -> sum(activeThreadsPerInstance);
        this.currentPoolSize = () -> sum(currentPoolSizePerInstance);
        this.maxPoolSize = () -> sum(maxPoolSizePerInstance);
        this.largestPoolSize = () -> sum(largestPoolSizePerInstance);
        this.queuedTaskCount = () -> sum(queuedTaskCountPerInstance);
        this.poolCount = executors::size;

        this.metrics = new IMetricValueProvider[]{
            totalRejectedCount,
            callerRunTaskCount,
            abortedTaskCount,
            discardedTaskCount,
            discardedOldestTaskCount,
            userRejectedPolicyCount,

            exceptionTaskCount,
            successfulTaskCount,
            totalTaskCount,
            minDuration,
            duration,
            maxDuration,

            activeThreads,
            currentPoolSize,
            maxPoolSize,
            largestPoolSize,
            queuedTaskCount,
            poolCount
        };
    }

    @Override
    public IMetricValueProvider[] getMetrics() {
        return metrics;
    }
}
