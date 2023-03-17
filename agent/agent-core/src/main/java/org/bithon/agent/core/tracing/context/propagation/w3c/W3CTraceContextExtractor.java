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

package org.bithon.agent.core.tracing.context.propagation.w3c;

import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.TraceContextFactory;
import org.bithon.agent.core.tracing.context.propagation.ITraceContextExtractor;
import org.bithon.agent.core.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.core.tracing.context.propagation.PropagationGetter;
import org.bithon.agent.core.tracing.context.propagation.TraceMode;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 10:00 上午
 */
public class W3CTraceContextExtractor implements ITraceContextExtractor {

    private static final int SAMPLED_FLAG = 0x1;

    @Override
    public <R> ITraceContext extract(R request, PropagationGetter<R> getter) {
        if (request == null) {
            return null;
        }

        String traceParent = getter.get(request, W3CTraceContextHeader.TRACE_HEADER_PARENT);
        if (traceParent == null) {
            return null;
        }

        String[] ids = traceParent.split("-");
        if (ids.length != 4) {
            return null;
        }

        // version
        if (ids[0].length() != 2) {
            return null;
        }

        // traceId
        if (ids[1].length() != 32) {
            return null;
        }

        // parent span id
        if (ids[2].length() != 16) {
            return null;
        }

        // trace flags
        if (ids[3].length() != 2) {
            return null;
        }

        ITraceContext context = TraceContextFactory.create(toTraceMode(ids[3]), ids[1], ids[2]);

        context.currentSpan()
               .parentApplication(getter.get(request, ITracePropagator.TRACE_HEADER_SRC_APPLICATION));
        return context;
    }

    private TraceMode toTraceMode(String id) {
        int flag = id.charAt(0) - '0' * 16 + (id.charAt(1) - '0');
        if ((flag & SAMPLED_FLAG) == SAMPLED_FLAG) {
            return TraceMode.TRACE;
        } else {
            return TraceMode.PROPAGATION;
        }
    }
}
