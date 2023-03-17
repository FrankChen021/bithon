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

package org.bithon.agent.core.tracing.context.propagation;

import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.propagation.w3c.W3CTraceContextInjector;
import org.bithon.agent.core.tracing.sampler.ISampler;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:56 下午
 */
public class DefaultPropagator implements ITracePropagator {

    /**
     * Always propagate W3C trace context for outgoing requests
     */
    private final ITraceContextInjector injector = new W3CTraceContextInjector();
    private final ITraceContextExtractor extractor;

    public DefaultPropagator(ISampler sampler) {
        extractor = new ChainedTraceContextExtractor(sampler);
    }

    @Override
    public <R> void inject(ITraceContext context, R request, PropagationSetter<R> setter) {
        injector.inject(context, request, setter);
    }

    @Override
    public <R> ITraceContext extract(R request, PropagationGetter<R> getter) {
        return extractor.extract(request, getter);
    }
}
