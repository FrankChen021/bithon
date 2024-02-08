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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
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
public class JobRunShell$Run extends AroundInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(JobRunShell$Run.class);

    private final ITraceContextExtractor extractor;

    public JobRunShell$Run() {
        ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                            .getDynamicConfig("tracing.samplingConfigs.quartz",
                                                                                              TraceSamplingConfig.class));
        this.extractor = new ChainedTraceContextExtractor(sampler);
    }

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceContext context = this.extractor.extract(null, null);
        if (context == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(context.currentSpan()
                                         .component("quartz")
                                         .kind(SpanKind.TIMER)
                                         .method(aopContext.getTargetClass(), aopContext.getMethod())
                                         .start());

        TraceContextHolder.set(context);

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
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
