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

package org.bithon.agent.plugin.thread.metrics;

import org.bithon.agent.observability.metric.collector.MetricCollectorManager;
import org.bithon.agent.observability.metric.collector.MetricRegistry;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.observability.metric.domain.thread.ThreadPoolMetrics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:13 下午
 */
public class ThreadPoolMetricRegistry extends MetricRegistry<ThreadPoolMetrics> {
    private static final String[] POOL_CLASS_EXCLUDE_PREFIX_LIST = {
        // A lambda class in the following class. it's a helper class which has no meaning to monitor it
        "org.springframework.cloud.commons.util.InetUtils"
    };
    static volatile ThreadPoolMetricRegistry INSTANCE;
    private final Map<AbstractExecutorService, List<String>> executors = new ConcurrentHashMap<>();

    public ThreadPoolMetricRegistry() {
        super("thread-pool-metrics",
              Arrays.asList("executorClass", "poolName"),
              ThreadPoolMetrics.class,
              null,
              false);
    }

    public static ThreadPoolMetricRegistry getInstance() {
        // See MetricCollectorManager for more detail to find why there's such a check below
        if (MetricCollectorManager.getInstance() == null) {
            return null;
        }

        if (INSTANCE == null) {
            INSTANCE = MetricRegistryFactory.getOrCreateRegistry("thread-pool-metrics", ThreadPoolMetricRegistry::new);
        }
        return INSTANCE;
    }

    public void addThreadPool(AbstractExecutorService pool, String executorClassName, String poolName, Supplier<ThreadPoolMetrics> metricsSupplier) {
        for (String excludePrefix : POOL_CLASS_EXCLUDE_PREFIX_LIST) {
            if (executorClassName.startsWith(excludePrefix)) {
                return;
            }
        }

        List<String> dimensions = Arrays.asList(executorClassName, poolName);
        this.getOrCreateMetrics(dimensions, metricsSupplier).add(pool);
        executors.put(pool, dimensions);
    }

    public void deleteThreadPool(AbstractExecutorService executor) {
        List<String> dimensions = executors.remove(executor);
        if (dimensions == null) {
            return;
        }
        ThreadPoolMetrics metrics = this.getMetrics(dimensions);
        if (metrics.remove(executor)) {
            this.removeMetrics(dimensions);
        }
    }

    private Optional<ThreadPoolMetrics> getMetrics(AbstractExecutorService executor) {
        List<String> dimensions = executors.get(executor);
        if (dimensions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.getMetrics(dimensions));
    }

    /**
     * @param duration millisecond
     */
    public void addRunCount(AbstractExecutorService executor,
                            long duration,
                            boolean hasException) {
        this.getMetrics(executor).ifPresent((metrics) -> {
            if (hasException) {
                metrics.exceptionTaskCount.incr();
            } else {
                metrics.successfulTaskCount.incr();
            }
            metrics.minDuration.update(duration);
            metrics.maxDuration.update(duration);
            metrics.duration.update(duration);
            metrics.totalTaskCount.incr();
        });
    }

    public void addTotal(AbstractExecutorService pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.totalTaskCount.incr());
    }

    public void addAbort(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> {
            metrics.abortedTaskCount.incr();
            metrics.totalRejectedCount.incr();
        });
    }

    public void addCallerRun(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> {
            metrics.callerRunTaskCount.incr();
            metrics.totalRejectedCount.incr();
        });
    }

    public void addDiscard(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> {
            metrics.discardedTaskCount.incr();
            metrics.totalRejectedCount.incr();
        });
    }

    public void addDiscardOldest(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> {
            metrics.discardedOldestTaskCount.incr();
            metrics.totalRejectedCount.incr();
        });
    }

    public void addUserPolicy(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> {
            metrics.userRejectedPolicyCount.incr();
            metrics.totalRejectedCount.incr();
        });
    }
}
