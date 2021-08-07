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

package com.sbss.bithon.agent.core.tracing.context;

import com.sbss.bithon.agent.bootstrap.expt.AgentException;
import com.sbss.bithon.agent.core.tracing.id.impl.DefaultSpanIdGenerator;
import com.sbss.bithon.agent.core.tracing.propagation.TraceMode;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 18:04
 */
public class TraceContextFactory {
    public static ITraceContext create(TraceMode traceMode, String traceId, String parentSpanId, String spanId) {
        ITraceContext ctx;
        switch (traceMode) {
            case TRACE:
                ctx = new TraceContext(traceId, new DefaultSpanIdGenerator());
                break;
            case PROPAGATION:
                ctx = new NoopTraceContext(traceId, new DefaultSpanIdGenerator());
                break;
            default:
                throw new AgentException("Unknown trace mode:%s", traceMode);
        }
        ctx.newSpan(parentSpanId, spanId);
        return ctx;
    }

    public static ITraceContext create(TraceMode traceMode, String traceId) {
        ITraceContext ctx;
        switch (traceMode) {
            case TRACE:
                ctx = new TraceContext(traceId, new DefaultSpanIdGenerator());
                ctx.newSpan();
                break;
            case PROPAGATION:
                ctx = new NoopTraceContext(traceId,
                                           new DefaultSpanIdGenerator());
                ctx.newSpan();
                break;
            default:
                throw new AgentException("Unknown trace mode:%s", traceMode);
        }
        ctx.newSpan();
        return ctx;
    }
}
