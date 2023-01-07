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

package org.bithon.agent.plugin.quartz2;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.core.tracing.Tracer;
import org.bithon.agent.core.tracing.config.TraceConfig;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceContextFactory;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.TraceMode;
import org.bithon.agent.core.tracing.sampler.ISampler;
import org.bithon.agent.core.tracing.sampler.SamplerFactory;
import org.bithon.agent.core.tracing.sampler.SamplingMode;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.quartz.JobExecutionException;
import org.quartz.core.JobRunShell;
import org.quartz.impl.JobExecutionContextImpl;

import java.lang.reflect.Field;

/**
 * @author frankchen
 */
public class JobRunShell$Run extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(JobRunShell$Run.class);

    private final ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                                      .getDynamicConfig("tracing.samplingConfigs.quartz",
                                                                                                        TraceConfig.SamplingConfig.class));

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

        aopContext.setUserContext(context.currentSpan()
                                         .component("quartz")
                                         .kind(SpanKind.TIMER)
                                         .method(aopContext.getMethod())
                                         .start());

        TraceContextHolder.set(context);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        JobExecutionContextImpl jobExecutionContext = null;
        try {
            JobRunShell jobRunShell = aopContext.getTargetAs();
            Field field = jobRunShell.getClass().getDeclaredField("jec");
            field.setAccessible(true);
            jobExecutionContext = (JobExecutionContextImpl) field.get(jobRunShell);
        } catch (NoSuchFieldException e) {
            log.warn("quartz jobContext field missing");
        } catch (IllegalAccessException e) {
            log.error("quartz jobContext field mismatch");
        }

        // assigned in NotifyJobListenersComplete
        IBithonObject bithonObject = aopContext.getTargetAs();
        JobExecutionException exception = (JobExecutionException) bithonObject.getInjectedObject();

        // tracing
        ITraceSpan span = aopContext.getUserContextAs();
        span.tag(exception == null ? null : exception.getUnderlyingException())
            .tag("status", exception != null ? "500" : "200")
            .tag("uri", jobExecutionContext == null ? null : "quartz://" + jobExecutionContext.getJobDetail().getJobClass().getName())
            .finish();
        span.context().finish();
        TraceContextHolder.remove();
    }
}
