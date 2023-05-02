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

package org.bithon.agent.plugin.spring.scheduling;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceSamplingConfig;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.sampler.ISampler;
import org.bithon.agent.observability.tracing.sampler.SamplerFactory;

/**
 * {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable#run()}
 * <p>
 * This class wraps the actual schedule runnable, we need to set up tracing context here,
 * so that the exception handling in {@link org.springframework.scheduling.support.DelegatingErrorHandlingRunnable#run()} can access the tracing context.
 * <p>
 * The span is actually created in {@link ScheduledMethodRunnable$Run}
 *
 * @author Frank Chen
 * @date 28/12/22 11:08 am
 */
public class DelegatingErrorHandlingRunnable$Run extends AroundInterceptor {
    private final ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                                      .getDynamicConfig("tracing.samplingConfigs.spring-scheduler",
                                                                                                        TraceSamplingConfig.class));

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        TraceContextHolder.set(TraceContextFactory.create(sampler.decideSamplingMode(null),
                                                          Tracer.get().traceIdGenerator().newTraceId()));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        TraceContextHolder.remove();
    }
}
