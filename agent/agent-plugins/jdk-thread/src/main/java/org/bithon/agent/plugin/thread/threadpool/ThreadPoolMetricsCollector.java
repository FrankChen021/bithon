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

package org.bithon.agent.plugin.thread.threadpool;

import org.bithon.agent.core.metric.collector.IntervalMetricCollector;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.thread.ThreadPoolMetrics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 9:13 下午
 */
public class ThreadPoolMetricsCollector extends IntervalMetricCollector<ThreadPoolMetrics> {
    private static final String[] POOL_CLASS_EXCLUDE_PREFIX_LIST = {
        // a lamda class in the following class. it's a helper class which has no meaning to monitor it
        "org.springframework.cloud.commons.util.InetUtils"
    };
    static volatile ThreadPoolMetricsCollector INSTANCE;
    private final Map<AbstractExecutorService, List<String>> executors = new ConcurrentHashMap<>();

    public ThreadPoolMetricsCollector() {
        super("thread-pool-metrics",
              Arrays.asList("executorClass", "poolName", "threadPoolId"),
              ThreadPoolMetrics.class,
              null,
              false);
    }

    public static ThreadPoolMetricsCollector getInstance() {
        // See MetricCollectorManager for more detail to find why there's such a check below
        if (MetricCollectorManager.getInstance() == null) {
            return null;
        }

        if (INSTANCE == null) {
            synchronized (ThreadPoolMetricsCollector.class) {
                if (INSTANCE == null) {
                    INSTANCE = MetricCollectorManager.getInstance().getOrRegister("thread-pool", ThreadPoolMetricsCollector.class);
                }
            }
        }
        return INSTANCE;
    }

    public void addThreadPool(AbstractExecutorService pool, String executorClassName, String poolName, ThreadPoolMetrics metrics) {
        for (String excludePrefix : POOL_CLASS_EXCLUDE_PREFIX_LIST) {
            if (executorClassName.startsWith(excludePrefix)) {
                return;
            }
        }

        List<String> dimensions = Arrays.asList(executorClassName, poolName, String.valueOf(System.identityHashCode(pool)));
        executors.put(pool, dimensions);
        this.register(dimensions, metrics);
    }

    public void deleteThreadPool(AbstractExecutorService executor) {
        List<String> dimensions = executors.remove(executor);
        if (dimensions == null) {
            return;
        }
        this.unregister(dimensions, true);
    }

    private Optional<ThreadPoolMetrics> getMetrics(AbstractExecutorService executor) {
        List<String> dimensions = executors.get(executor);
        if (dimensions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.getMetrics(dimensions));
    }

    public void addRunCount(AbstractExecutorService executor,
                            boolean hasException) {
        this.getMetrics(executor).ifPresent((metrics) -> {
            if (hasException) {
                metrics.exceptionTaskCount.incr();
            } else {
                metrics.successfulTaskCount.incr();
            }
            metrics.totalTaskCount.incr();
        });
    }

    public void addTotal(AbstractExecutorService pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.totalTaskCount.incr());
    }

    public void addAbort(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.abortedTaskCount.incr());
    }

    public void addCallerRun(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.callerRunTaskCount.incr());
    }

    public void addDiscard(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.discardedTaskCount.incr());
    }

    public void addDiscardOldest(ThreadPoolExecutor pool) {
        this.getMetrics(pool).ifPresent((metrics) -> metrics.discardedOldestTaskCount.incr());
    }
}
