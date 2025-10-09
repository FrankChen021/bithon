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
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.ChainedTraceContextExtractor;
import org.bithon.agent.observability.tracing.context.propagation.ITraceContextExtractor;
import org.bithon.agent.observability.tracing.sampler.ISampler;
import org.bithon.agent.observability.tracing.sampler.SamplerFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.consumer.config.KafkaConsumerTraceSamplingConfig;
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

    private final ITraceContextExtractor extractor;

    public ListenerConsumer$PollAndInvoke() {
        ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                            .getConfig(KafkaConsumerTraceSamplingConfig.class));
        this.extractor = new ChainedTraceContextExtractor(sampler);
    }

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getTargetAs();
        KafkaPluginContext pluginContext = (KafkaPluginContext) bithonObject.getInjectedObject();
        if (pluginContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ITraceContext context = TraceContextHolder.attach(this.extractor.extract(null, null));
        if (context == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // Start the span even if this span is the type of TraceMode.LOGGING
        // so that the trace id can be injected into MDC for logging only
        aopContext.setSpan(context.currentSpan()
                                  .name(Components.KAFKA)
                                  .kind(SpanKind.CONSUMER)
                                  .method(aopContext.getTargetClass(), aopContext.getMethod())
                                  .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getTargetAs();
        KafkaPluginContext pluginContext = (KafkaPluginContext) bithonObject.getInjectedObject();

        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException())
            .tag("status", aopContext.hasException() ? "500" : "200")
            .tag("uri", pluginContext.uri)
            .tag(Tags.Net.PEER, pluginContext.broker)
            .tag(Tags.Messaging.KAFKA_TOPIC, pluginContext.topic)
            .tag(Tags.Messaging.KAFKA_CONSUMER_GROUP, pluginContext.groupId)
            .tag(Tags.Messaging.KAFKA_CLIENT_ID, pluginContext.clientId)
            .finish();
        span.context().finish();

        TraceContextHolder.detach();
    }
}
