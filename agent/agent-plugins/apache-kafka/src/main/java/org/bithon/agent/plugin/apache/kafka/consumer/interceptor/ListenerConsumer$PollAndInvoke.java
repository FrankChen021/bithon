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

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.config.ConfigurationManager;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.config.TraceSamplingConfig;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.TraceMode;
import org.bithon.agent.observability.tracing.sampler.ISampler;
import org.bithon.agent.observability.tracing.sampler.SamplerFactory;
import org.bithon.agent.observability.tracing.sampler.SamplingMode;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.TopicPartitionOffset;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link org.springframework.kafka.listener.KafkaMessageListenerContainer$ListenerConsumer}
 *
 * @author Frank Chen
 * @date 28/11/22 8:47 pm
 */
public class ListenerConsumer$PollAndInvoke extends AbstractInterceptor {

    private final ISampler sampler = SamplerFactory.createSampler(ConfigurationManager.getInstance()
                                                                                      .getDynamicConfig("tracing.samplingConfigs.kafka-consumer",
                                                                                                        TraceSamplingConfig.class));

    /**
     * Keep topic information on injected fields for further use
     */
    @Override
    public void onConstruct(AopContext aopContext) {
        KafkaConsumer<?, ?> consumer = (KafkaConsumer<?, ?>) ReflectionUtils.getFieldValue(aopContext.getTarget(), "consumer");
        if (consumer == null) {
            return;
        }
        String cluster = ((KafkaPluginContext) ((IBithonObject) consumer).getInjectedObject()).clusterSupplier.get();

        ContainerProperties properties = (ContainerProperties) ReflectionUtils.getFieldValue(aopContext.getTarget(), "containerProperties");
        if (properties == null) {
            return;
        }

        String uri = "kafka://" + cluster;
        String[] topics = properties.getTopics();
        if (topics != null) {
            uri += "?topic=" + String.join(",", topics);
        } else if (properties.getTopicPattern() != null) {
            uri += "?topic=" + properties.getTopicPattern().pattern();
        } else {
            TopicPartitionOffset[] partitions = properties.getTopicPartitions();
            if (partitions != null) {
                uri += "?topic=" + Stream.of(partitions).map(TopicPartitionOffset::getTopic).collect(Collectors.joining(","));
            }
        }

        // Keep the uri for further use
        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(uri);
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceContext context;
        SamplingMode mode = sampler.decideSamplingMode(null);
        if (mode == SamplingMode.NONE) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // create a traceable context
        context = TraceContextFactory.create(TraceMode.TRACE,
                                             Tracer.get().traceIdGenerator().newTraceId());

        aopContext.setUserContext(context.currentSpan()
                                         .component("kafka")
                                         .kind(SpanKind.CONSUMER)
                                         .method(aopContext.getMethod())
                                         .start());

        TraceContextHolder.set(context);
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();

        span.tag(aopContext.getException())
            .tag("status", aopContext.hasException() ? "500" : "200")
            .tag("uri", ((IBithonObject) aopContext.getTarget()).getInjectedObject())
            .finish();
        span.context().finish();

        TraceContextHolder.remove();
    }
}
