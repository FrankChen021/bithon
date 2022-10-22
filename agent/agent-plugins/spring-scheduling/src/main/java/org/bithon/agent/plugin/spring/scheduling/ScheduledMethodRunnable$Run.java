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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.TraceContextFactory;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.TraceMode;
import org.bithon.agent.core.tracing.sampler.ISampler;
import org.bithon.agent.core.tracing.sampler.SamplerFactory;
import org.bithon.agent.core.tracing.sampler.SamplingMode;
import org.bithon.component.commons.tracing.SpanKind;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

/**
 * {@link org.springframework.scheduling.support.ScheduledMethodRunnable#run()}
 *
 * @author frankchen
 * @date 13/03/2022 13:45
 */
public class ScheduledMethodRunnable$Run extends AbstractInterceptor {

    private ISampler sampler;

    @Override
    public boolean initialize() {
        TraceConfig traceConfig = AgentContext.getInstance()
                                              .getAgentConfiguration()
                                              .getConfig(TraceConfig.class);
        TraceConfig.SamplingConfig samplingConfig = traceConfig.getSamplingConfigs().get("spring-scheduler");
        if (samplingConfig == null || samplingConfig.isDisabled() || samplingConfig.getSamplingRate() == 0) {
            return false;
        }

        sampler = SamplerFactory.createSampler(samplingConfig);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceContext context;
        SamplingMode mode = sampler.decideSamplingMode(null);
        if (mode == SamplingMode.NONE) {
            return InterceptionDecision.SKIP_LEAVE;
        } else {
            // create a traceable context
            context = TraceContextFactory.create(TraceMode.TRACE,
                                                 Tracer.get().traceIdGenerator().newTraceId());
        }

        ScheduledMethodRunnable runnable = aopContext.castTargetAs();
        context.currentSpan()
               .component("springScheduler")
               .kind(SpanKind.SERVER)
               .method(aopContext.getMethod())
               .tag("uri", "/scheduler/" + runnable.getTarget().getClass().getName().replace('.', '/') + "/" + runnable.getMethod().getName())
               .start();

        aopContext.setUserContext(context);
        TraceContextHolder.set(context);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceContext context = aopContext.castUserContextAs();

        context.currentSpan()
               .tag(aopContext.getException())
               .tag("status", aopContext.hasException() ? "500" : "200")
               .finish();
        context.finish();

        TraceContextHolder.remove();
    }
}
