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

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/7 8:44 下午
 */
public class TraceSpanFactory {

    public static ITraceSpan newSpan(String name) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return null;
        }

        ITraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return null;
        }

        // create a span and save it in user-context
        return parentSpan.newChildSpan(name);
    }

    public static ITraceSpan newAsyncSpan(String name) {
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext == null) {
            return NullTraceSpan.INSTANCE;
        }

        ITraceSpan parentSpan = traceContext.currentSpan();
        if (parentSpan == null) {
            return NullTraceSpan.INSTANCE;
        }

        //TODO: provide a 'clone' on TraceContext to eliminate this instanceOf checking
        if (traceContext instanceof PropagationTraceContext) {
            return new PropagationTraceContext(traceContext.traceId(),
                                               traceContext.spanIdGenerator())
                .reporter(traceContext.reporter())
                .newSpan(parentSpan.parentSpanId(), parentSpan.spanId())
                .component(name);
        } else {
            return new TraceContext(traceContext.traceId(),
                                    traceContext.spanIdGenerator())
                .reporter(traceContext.reporter())
                .newSpan(parentSpan.spanId(), traceContext.spanIdGenerator().newSpanId())
                .component(name);
        }
    }
}
