package com.sbss.bithon.agent.core.metric;

import com.sbss.bithon.agent.core.context.AgentContext;
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
 * @author frankchen
 */
public class MetricCollectorManager {
    private static final Logger log = LoggerFactory.getLogger(MetricCollectorManager.class);

    private static final int INTERVAL = 10;
    private static final MetricCollectorManager INSTANCE = new MetricCollectorManager();
    private final ConcurrentMap<String, IMetricCollector> providers;
    private final Dispatcher dispatcher;
    ScheduledExecutorService scheduler;

    private MetricCollectorManager() {
        this.providers = new ConcurrentHashMap<>();
        this.dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRICS);

        // NOTE:
        // Constructing ScheduledThreadPoolExecutor would cause ThreadPoolInterceptor be executed
        // And ThreadPoolInterceptor then would call getInstance of this class which would return NULL
        // because the constructing process of this class has not completed
        this.scheduler = new ScheduledThreadPoolExecutor(2,
                                                         NamedThreadFactory.of("bithon-metric-collector"),
                                                         new ThreadPoolExecutor.CallerRunsPolicy());
        this.scheduler.scheduleWithFixedDelay(this::collectorAndDispatch, 0, INTERVAL, TimeUnit.SECONDS);
    }

    public static MetricCollectorManager getInstance() {
        return INSTANCE;
    }

    public boolean isProviderExists(String name) {
        for (String providerName : providers.keySet()) {
            if (providerName.contains(name)) {
                return true;
            }
        }
        return false;
    }

    public <T extends IMetricCollector> T register(String providerName, T provider) {
        if (providers.containsKey(providerName)) {
            throw new RuntimeException(String.format("Metrics Local Storage(%s) already registered!", providerName));
        } else {
            providers.put(providerName, provider);
            log.debug(String.format("Success to register metrics local storage(%s)", providerName));
        }
        return provider;
    }

    @SuppressWarnings("unchecked")
    public <T extends IMetricCollector> T getOrRegister(String providerName, Class<T> providerClass) {
        IMetricCollector provider = providers.get(providerName);
        if (provider != null) {
            return (T) provider;
        }
        try {
            return register(providerName, providerClass.newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Can't create or register metric provider " + providerName, e);
        }
    }

    private void collectorAndDispatch() {
        if (!dispatcher.isReady()) {
            return;
        }

        for (IMetricCollector provider : providers.values()) {
            if (provider.isEmpty()) {
                continue;
            }

            this.scheduler.execute(()->{
                try {
                    List<Object> messages = provider.collect(dispatcher.getMessageConverter(),
                                                             AgentContext.getInstance().getAppInstance(),
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

