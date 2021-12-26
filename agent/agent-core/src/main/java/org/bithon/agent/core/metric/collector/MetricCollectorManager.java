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

package org.bithon.agent.core.metric.collector;

import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.utils.CollectionUtils;
import org.bithon.agent.core.utils.concurrent.NamedThreadFactory;
import org.bithon.component.logging.ILogAdaptor;
import org.bithon.component.logging.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Metric Registry and Dispatcher(in some other system, it's called as reporter)
 *
 * @author frankchen
 */
public class MetricCollectorManager {
    private static final ILogAdaptor log = LoggerFactory.getLogger(MetricCollectorManager.class);

    private static final int INTERVAL = 10;
    private static final MetricCollectorManager INSTANCE = new MetricCollectorManager();
    private final ConcurrentMap<String, ManagedMetricCollector> collectors;
    private final Dispatcher dispatcher;
    ScheduledExecutorService scheduler;

    static class ManagedMetricCollector {
        /**
         * last timestamp when the collector is scheduled to collect metrics
         */
        long lastCollectedAt;

        /**
         * interval of this period in second
         */
        int interval;

        IMetricCollectorBase collector;

        public ManagedMetricCollector(IMetricCollectorBase collector) {
            this.collector = collector;
        }

        public Object collect(IMessageConverter messageConverter) {
            if (collector instanceof IMetricCollector) {
                List<Object> objects = ((IMetricCollector) this.collector).collect(messageConverter, interval, lastCollectedAt);
                if (CollectionUtils.isEmpty(objects)) {
                    return null;
                }
                return objects;
            }
            return ((IMetricCollector2) collector).collect(messageConverter, interval, lastCollectedAt);
        }

        public boolean isEmpty() {
            if (lastCollectedAt != 0) {
                long now = System.currentTimeMillis();
                interval = (int) ((now - lastCollectedAt) / 1000);
                this.lastCollectedAt = now;
                return this.collector.isEmpty();
            } else {
                // wait for next round
                lastCollectedAt = System.currentTimeMillis();
                return false;
            }
        }
    }

    private MetricCollectorManager() {
        this.collectors = new ConcurrentHashMap<>();
        this.dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRIC);

        // NOTE:
        // Constructing ScheduledThreadPoolExecutor would cause ThreadPoolInterceptor be executed
        // And ThreadPoolInterceptor then would call getInstance of this class which would return NULL
        // because the constructing process of this class has not completed
        this.scheduler = new ScheduledThreadPoolExecutor(2,
                                                         NamedThreadFactory.of("bithon-metric-collector"),
                                                         new ThreadPoolExecutor.CallerRunsPolicy());
        this.scheduler.scheduleWithFixedDelay(this::collectAndDispatch, 0, INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * call of this method in plugins' initialization might return NULL
     * This is because this class is still being constructing and the construction triggers some classes to be load,
     * and these classes are transformed to be delegated to plugins' interceptors
     */
    public static MetricCollectorManager getInstance() {
        return INSTANCE;
    }

    public boolean collectorExists(String name) {
        for (String providerName : collectors.keySet()) {
            if (providerName.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized <T extends IMetricCollectorBase> T register(String collectorName, T collector) {
        if (collectors.containsKey(collectorName)) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "Metrics Local Storage(%s) already registered!", collectorName));
        }

        //noinspection unchecked
        return (T) collectors.computeIfAbsent(collectorName, key -> new ManagedMetricCollector(collector)).collector;
    }

    @SuppressWarnings("unchecked")
    public <T extends IMetricCollectorBase> T getOrRegister(String collectorName, Class<T> collectorClass) {
        ManagedMetricCollector managedCollector = collectors.get(collectorName);
        if (managedCollector != null) {
            return (T) managedCollector.collector;
        }
        synchronized (this) {
            try {
                managedCollector = collectors.get(collectorName);
                // double check
                if (managedCollector != null) {
                    return (T) managedCollector.collector;
                }

                managedCollector = new ManagedMetricCollector(collectorClass.newInstance());
                collectors.put(collectorName, managedCollector);
                return (T) managedCollector.collector;
            } catch (Exception e) {
                throw new RuntimeException("Can't create or register metric provider " + collectorName, e);
            }
        }
    }

    public void unregister(String name) {
        collectors.remove(name);
    }

    private void collectAndDispatch() {
        if (!dispatcher.isReady()) {
            return;
        }

        for (ManagedMetricCollector managedCollector : collectors.values()) {
            if (managedCollector.isEmpty()) {
                continue;
            }

            this.scheduler.execute(() -> {
                try {
                    Object message = managedCollector.collect(dispatcher.getMessageConverter());
                    if (message != null) {
                        dispatcher.sendMessage(message);
                    }
                } catch (Throwable e) {
                    log.error("Throwable(unrecoverable) exception occurred when dispatching!", e);
                }
            });
        }
    }
}

