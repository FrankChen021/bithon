package com.sbss.bithon.agent.core.metric.collector;

import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.utils.CollectionUtils;
import com.sbss.bithon.agent.core.utils.concurrent.NamedThreadFactory;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
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
    private static final Logger log = LoggerFactory.getLogger(MetricCollectorManager.class);

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

        IMetricCollector collector;

        public ManagedMetricCollector(IMetricCollector collector) {
            this.collector = collector;
        }

        public List<Object> collect(IMessageConverter messageConverter) {
            return this.collector.collect(messageConverter, interval, lastCollectedAt);
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

    public <T extends IMetricCollector> T register(String collectorName, T collector) {
        if (collectors.containsKey(collectorName)) {
            throw new RuntimeException(String.format("Metrics Local Storage(%s) already registered!", collectorName));
        }

        collectors.computeIfAbsent(collectorName, key -> new ManagedMetricCollector(collector));

        return collector;
    }

    @SuppressWarnings("unchecked")
    public <T extends IMetricCollector> T getOrRegister(String collectorName, Class<T> collectorClass) {
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
                    List<Object> messages = managedCollector.collect(dispatcher.getMessageConverter());
                    if (CollectionUtils.isNotEmpty(messages)) {
                        dispatcher.sendMessage(messages);
                    }
                } catch (Throwable e) {
                    log.error("Throwable(unrecoverable) exception occurred when dispatching!", e);
                }
            });
        }
    }
}

