package com.sbss.bithon.agent.core.metric;

import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
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
    private final ConcurrentMap<String, IMetricCollector> collectors;
    private final Dispatcher dispatcher;
    ScheduledExecutorService scheduler;

    private MetricCollectorManager() {
        this.collectors = new ConcurrentHashMap<>();
        this.dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRICS);

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
        if (collectors.putIfAbsent(collectorName, collector) != null) {
            throw new RuntimeException(String.format("Metrics Local Storage(%s) already registered!", collectorName));
        }
        return collector;
    }

    @SuppressWarnings("unchecked")
    public <T extends IMetricCollector> T getOrRegister(String collectorName, Class<T> collectorClass) {
        IMetricCollector collector = collectors.get(collectorName);
        if (collector != null) {
            return (T) collector;
        }
        synchronized (this) {
            try {
                collector = collectors.get(collectorName);
                // double check
                if (collector != null) {
                    return (T) collector;
                }

                collector = collectorClass.newInstance();
                collectors.put(collectorName, collector);
                return (T) collector;
            } catch (Exception e) {
                throw new RuntimeException("Can't create or register metric provider " + collectorName, e);
            }
        }
    }

    private void collectAndDispatch() {
        if (!dispatcher.isReady()) {
            return;
        }

        for (IMetricCollector collector : collectors.values()) {
            if (collector.isEmpty()) {
                continue;
            }

            this.scheduler.execute(() -> {
                try {
                    List<Object> messages = collector.collect(dispatcher.getMessageConverter(),
                                                              INTERVAL,
                                                              System.currentTimeMillis());
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

