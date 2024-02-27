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

package org.bithon.agent.observability.tracing.context;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 18:38
 */
public class TraceContextListener {

    private static final TraceContextListener INSTANCE = new TraceContextListener();
    private final List<IListener> listeners = new CopyOnWriteArrayList<>();

    public static TraceContextListener getInstance() {
        return INSTANCE;
    }

    private final IListener spanDebugListener = new SpanEventDebugLogger();

    public TraceContextListener() {
        ConfigurationManager.getInstance()
                            .addConfigurationChangedListener("tracing.debug", this::addOrRemoveDebugListener);
        addOrRemoveDebugListener();
    }

    private void addOrRemoveDebugListener() {
        TraceConfig traceConfig = ConfigurationManager.getInstance()
                                                      .getConfig(TraceConfig.class);
        boolean existed = listeners.contains(spanDebugListener);
        if (traceConfig.isDebug()) {
            if (!existed) {
                // flag changes from false to true, add the listener
                listeners.add(spanDebugListener);
            }
        } else {
            if (existed) {
                // flag changes from true to false, remove the listener
                listeners.remove(spanDebugListener);
            }
        }
    }

    public void addListener(IListener listener) {
        listeners.add(listener);
    }

    public void onSpanStarted(ITraceSpan span) {
        for (IListener listener : listeners) {
            listener.onSpanStarted(span);
        }
    }

    public void onSpanFinished(ITraceSpan span) {
        for (IListener listener : listeners) {
            listener.onSpanFinished(span);
        }
    }

    public interface IListener extends EventListener {
        void onSpanStarted(ITraceSpan span);

        void onSpanFinished(ITraceSpan span);
    }

    private static class SpanEventDebugLogger implements IListener {
        private final ILogAdaptor log = LoggerFactory.getLogger(TraceContextListener.class);

        @Override
        public void onSpanStarted(ITraceSpan span) {
            log.info("[Created] {}", span);
        }

        @Override
        public void onSpanFinished(ITraceSpan span) {
            log.info("[Finished] {}", span);
        }
    }
}
