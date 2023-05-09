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

package org.bithon.agent.plugin.apache.kafka.consumer.interceptor;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceSamplingConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;
import org.bithon.agent.observability.tracing.sampler.ISampler;
import org.bithon.agent.observability.tracing.sampler.SamplerFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * {@link org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer}
 *
 * @author Frank Chen
 * @date 28/11/22 8:47 pm
 */
public class ListenerConsumer$PollAndInvoke extends AroundInterceptor {

    private final ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                                      .getDynamicConfig(
                                                                                          "tracing.samplingConfigs.kafka-consumer",
                                                                                          TraceSamplingConfig.class));

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getTargetAs();
        KafkaPluginContext pluginContext = (KafkaPluginContext) bithonObject.getInjectedObject();
        if (pluginContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceContext context = TraceContextFactory.create(sampler.decideSamplingMode(null),
                                                           Tracer.get().traceIdGenerator().newTraceId());

        if (TraceMode.TRACING.equals(context.traceMode())) {
            aopContext.setUserContext(context.currentSpan()
                                             .component(Components.KAFKA)
                                             .kind(SpanKind.CONSUMER)
                                             .method(aopContext.getTargetClass(), aopContext.getMethod())
                                             .start());
        }

        TraceContextHolder.set(context);
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();

        if (span != null) {
            IBithonObject bithonObject = aopContext.getTargetAs();
            KafkaPluginContext pluginContext = (KafkaPluginContext) bithonObject.getInjectedObject();

            span.tag(aopContext.getException())
                .tag("status", aopContext.hasException() ? "500" : "200")
                .tag("uri", ((IBithonObject) aopContext.getTarget()).getInjectedObject())
                .tag(Tags.Net.PEER, pluginContext.clusterSupplier.get())
                .tag(Tags.Messaging.KAFKA_TOPIC, pluginContext.topic)
                .tag(Tags.Messaging.KAFKA_CONSUMER_GROUP, pluginContext.groupId)
                .tag(Tags.Messaging.KAFKA_CLIENT_ID, pluginContext.clientId)
                .finish();

            span.context().finish();
        }

        TraceContextHolder.remove();
    }
}
