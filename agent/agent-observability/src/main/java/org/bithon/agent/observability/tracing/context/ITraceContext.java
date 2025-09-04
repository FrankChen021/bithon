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

import org.bithon.agent.observability.tracing.context.propagation.PropagationSetter;
import org.bithon.agent.observability.tracing.id.ISpanIdGenerator;
import org.bithon.component.commons.time.Clock;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/17 3:01 下午
 */
public interface ITraceContext {

    default ITraceContext traceState(TraceState attributes) {
        return this;
    }
    default TraceState traceState() {
        return null;
    }

    TraceMode traceMode();

    String traceId();

    ITraceSpan currentSpan();

    Clock clock();

    ISpanIdGenerator spanIdGenerator();

    default ITraceSpan newSpan() {
        return newSpan(null, spanIdGenerator().newSpanId());
    }

    default ITraceSpan newSpan(String parentSpanId) {
        return newSpan(parentSpanId, spanIdGenerator().newSpanId());
    }

    ITraceSpan newSpan(String parentSpanId, String spanId);

    void finish();

    <T> void propagate(T injectedTo, PropagationSetter<T> setter);

    ITraceContext copy();

    boolean finished();
}
