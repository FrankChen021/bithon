package com.sbss.bithon.agent.core.metrics;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.utils.CollectionUtils;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author frankchen
 */
public class MetricProviderManager {
    private static final Logger log = LoggerFactory.getLogger(MetricProviderManager.class);

    private static final int INTERVAL = 10;
    private final ConcurrentMap<String, IMetricProvider> providers;
    private final Dispatcher dispatcher;

    private static final MetricProviderManager INSTANCE = new MetricProviderManager();

    public boolean isProviderExists(String name) {
        for (String providerName : providers.keySet()) {
            if (providerName.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private MetricProviderManager() {
        this.providers = new ConcurrentHashMap<>();
        this.dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRICS);

        // Collect metrics
        new Timer("metrics-collector").schedule(new TimerTask() {
            @Override
            public void run() {
                if (!dispatcher.isReady()) {
                    return;
                }

                for (IMetricProvider provider : providers.values()) {
                    if (provider.isEmpty()) {
                        continue;
                    }

                    try {
                        List<Object> messages = provider.buildMessages(dispatcher.getMessageConverter(),
                                AgentContext.getInstance().getAppInstance(),
                                INTERVAL,
                                System.currentTimeMillis());
                        if (CollectionUtils.isNotEmpty(messages)) {
                            dispatcher.sendMessage(messages);
                        }
                    } catch (Throwable e) {
                        log.error("Throwable(unrecoverable) exception occurred when dispatching!", e);
                    }
                }
            }
        }, 0, INTERVAL * 1000);
    }

    public static MetricProviderManager getInstance() {
        return INSTANCE;
    }

    public <T extends IMetricProvider> T register(String providerName, T provider) {
        if (providers.containsKey(providerName)) {
            throw new RuntimeException(String.format("Metrics Local Storage(%s) already registered!", providerName));
        } else {
            providers.put(providerName, provider);
            log.debug(String.format("Success to register metrics local storage(%s)", providerName));
        }
        return provider;
    }

    @SuppressWarnings("unchecked")
    public <T extends IMetricProvider> T getOrRegister(String providerName, Class<T> providerClass) {
        IMetricProvider provider = providers.get(providerName);
        if (provider != null) {
            return (T) provider;
        }
        try {
            return register(providerName, providerClass.newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Can't create or register metric provider " + providerName, e);
        }
    }
}

