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
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.propagation.ITracePropagator;
import com.sbss.bithon.agent.core.tracing.sampling.SamplingMode;

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
    public <R> TraceContext extract(R request, PropagationGetter<R> getter) {
        //
        // TODO: sampling decision making first
        //
        SamplingMode mode = Tracer.get().samplingDecisionMaker().decideSamplingMode(request);
        if (mode == SamplingMode.NONE) {
            return null;
        }

        for (ITraceContextExtractor extractor : extractors) {
            TraceContext context = extractor.extract(request, getter);
            if (context != null) {
                return context.samplingMode(mode);
            }
        }

        // context decide by SamplingMode
        TraceContext context = new TraceContext(Tracer.get().traceIdGenerator().newTraceId(),
                                                Tracer.get().reporter(),
                                                Tracer.get().traceIdGenerator());
        context.currentSpan()
               .parentApplication(getter.get(request, ITracePropagator.BITHON_SOURCE_APPLICATION));
        return context;
    }

}
