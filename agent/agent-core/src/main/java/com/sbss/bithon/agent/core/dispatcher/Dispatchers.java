package com.sbss.bithon.agent.core.dispatcher;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.dispatcher.channel.IMessageChannelFactory;
import shaded.org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/21 11:02 下午
 */
public class Dispatchers {
    /**
     * the name MUST corresponds to the name of methods such as {@link IMessageChannelFactory#createMetricChannel(DispatcherConfig)}
     */
    public static final String DISPATCHER_NAME_METRIC = "metric";
    public static final String DISPATCHER_NAME_TRACING = "tracing";
    public static final String DISPATCHER_NAME_EVENT = "event";

    private static final Map<String, Dispatcher> DISPATCHERS = new HashMap<>();

    private static Dispatchers INSTANCE;

    public static Dispatchers getInstance() {
        return INSTANCE;
    }

    public static Dispatcher getOrCreate(String dispatcherName) {
        DispatcherConfig config = AgentContext.getInstance().getConfig().getDispatchers().get(dispatcherName);
        if (config == null) {
            return null;
        }
        Dispatcher dispatcher = DISPATCHERS.get(dispatcherName);
        if (dispatcher != null) {
            return dispatcher;
        }

        synchronized (DISPATCHERS) {
            // double check
            dispatcher = DISPATCHERS.get(dispatcherName);
            if (dispatcher != null) {
                return dispatcher;
            }

            return DISPATCHERS.computeIfAbsent(dispatcherName, key -> {
                try {
                    return new Dispatcher(dispatcherName,
                                          AgentContext.getInstance().getAgentDirectory(),
                                          AgentContext.getInstance().getAppInstance(),
                                          config);
                } catch (Exception e) {
                    LoggerFactory.getLogger(Dispatchers.class)
                                 .error("Failed to create dispatcher: " + dispatcherName, e);
                    return null;
                }
            });
        }
    }
}
