/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.core.tracing.context;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 18:38
 */
public class TraceContextListener {

    private static final TraceContextListener INSTANCE = new TraceContextListener();
    private final List<IListener> listeners = new ArrayList<>();

    public static TraceContextListener getInstance() {
        return INSTANCE;
    }

    public synchronized void addListener(IListener listener) {
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
}
