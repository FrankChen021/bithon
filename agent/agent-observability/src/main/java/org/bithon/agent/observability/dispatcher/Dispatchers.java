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

package org.bithon.agent.observability.dispatcher;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.dispatcher.channel.IMessageChannelFactory;
import org.bithon.agent.observability.dispatcher.config.DispatcherConfig;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/21 11:02 下午
 */
public class Dispatchers {
    /**
     * the name MUST correspond to the name of methods such as {@link IMessageChannelFactory#createMetricChannel(DispatcherConfig)}
     */
    public static final String DISPATCHER_NAME_METRIC = "metric";
    public static final String DISPATCHER_NAME_TRACING = "tracing";
    public static final String DISPATCHER_NAME_EVENT = "event";

    private static final Map<String, Dispatcher> DISPATCHERS = new HashMap<>();

    public static Collection<Dispatcher> getAllDispatcher() {
        synchronized (DISPATCHERS) {
            return new ArrayList<>(DISPATCHERS.values());
        }
    }

    public static Dispatcher getOrCreate(String dispatcherName) {
        DispatcherConfig config = ConfigurationManager.getInstance()
                                                      .getConfig("dispatchers." + dispatcherName, DispatcherConfig.class, true);
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
                                          AppInstance.getInstance(),
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
