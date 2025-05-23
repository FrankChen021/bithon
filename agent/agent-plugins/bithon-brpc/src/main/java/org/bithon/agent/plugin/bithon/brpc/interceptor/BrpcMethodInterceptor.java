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

package org.bithon.agent.plugin.bithon.brpc.interceptor;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.config.TraceSamplingConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.ChainedTraceContextExtractor;
import org.bithon.agent.observability.tracing.context.propagation.ITraceContextExtractor;
import org.bithon.agent.observability.tracing.sampler.ISampler;
import org.bithon.agent.observability.tracing.sampler.SamplerFactory;
import org.bithon.component.commons.tracing.SpanKind;

/**
 * @author frank.chen021@outlook.com
 * @date 12/5/22 10:25 PM
 */
public class BrpcMethodInterceptor extends AroundInterceptor {

    private final ITraceContextExtractor extractor;

    public BrpcMethodInterceptor() {
        ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                            .getDynamicConfig("tracing.samplingConfigs.brpc",
                                                                                              TraceSamplingConfig.class));
        this.extractor = new ChainedTraceContextExtractor(sampler);
    }

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceContext context = this.extractor.extract(null, null);
        if (context == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setSpan(context.currentSpan()
                                  .name("brpc")
                                  .kind(SpanKind.SERVER)
                                  .method(aopContext.getTargetClass(), aopContext.getMethod())
                                  .tag("uri", "brpc://" + aopContext.getTarget().getClass().getSimpleName() + "/" + aopContext.getMethod())
                                  .start());

        TraceContextHolder.attach(context);
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException())
            .tag("status", aopContext.hasException() ? "500" : "200")
            .finish();
        span.context().finish();

        TraceContextHolder.detach();
    }
}
