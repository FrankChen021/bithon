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

package com.sbss.bithon.agent.core.tracing.propagation.extractor;

import com.sbss.bithon.agent.core.tracing.Tracer;
import com.sbss.bithon.agent.core.tracing.context.ITraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextFactory;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import com.sbss.bithon.agent.core.tracing.propagation.TraceMode;
import com.sbss.bithon.agent.core.tracing.sampler.SamplingMode;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:59 下午
 */
public class ChainedTraceContextExtractor implements ITraceContextExtractor {

    private final ITraceContextExtractor[] extractors = new ITraceContextExtractor[]{
        new B3Extractor(),
        new BithonExtractor(),
        };

    @Override
    public <R> ITraceContext extract(R request, PropagationGetter<R> getter) {
        //
        // extract trace context based on upstream service request
        //
        for (ITraceContextExtractor extractor : extractors) {
            ITraceContext context = extractor.extract(request, getter);
            if (context != null) {
                return context;
            }
        }

        //
        // no trace context,
        // then handle to sampling decision maker to decide whether or not this request should be sampled
        //
        ITraceContext context;
        SamplingMode mode = Tracer.get().sampler().decideSamplingMode(request);
        if (mode == SamplingMode.NONE) {
            // create a propagation trace context to propagation trace context along the service call without reporting trace data
            context = TraceContextFactory.create(TraceMode.PROPAGATION,
                                                 "P-" + Tracer.get().traceIdGenerator().newTraceId());
        } else {
            // create a traceable context
            context = TraceContextFactory.create(TraceMode.TRACE,
                                                 Tracer.get().traceIdGenerator().newTraceId());
        }

        context.currentSpan()
               .parentApplication(getter.get(request, ITracePropagator.BITHON_SRC_APPLICATION));
        return context;
    }

}
