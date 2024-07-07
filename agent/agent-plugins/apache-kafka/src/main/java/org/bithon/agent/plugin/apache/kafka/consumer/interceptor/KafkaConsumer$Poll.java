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

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.Tags;

import java.time.Duration;

/**
 * {@link org.apache.kafka.clients.consumer.KafkaConsumer#poll(Duration)}
 *
 * @author Frank Chen
 * @date 28/11/22 8:39 pm
 */
public class KafkaConsumer$Poll extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        // To be compatible with Kafka consumer before 3.x,
        // The interceptor is installed on all 'poll' methods,
        // The following check prevents recursive interception
        if (InterceptorContext.get("kafka.consumer.context") != null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // So that the CompletedFetch can get the context
        KafkaPluginContext pluginContext = aopContext.getInjectedOnTargetAs();
        InterceptorContext.set("kafka.consumer.context", pluginContext);

        ITraceSpan span = TraceContextFactory.newSpan(Components.KAFKA);
        if (span != null) {
            aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                   .tag("uri", pluginContext.uri)
                                   .tag(Tags.Net.PEER, pluginContext.clusterSupplier.get())
                                   .tag(Tags.Messaging.KAFKA_TOPIC, pluginContext.topic)
                                   .tag(Tags.Messaging.KAFKA_CONSUMER_GROUP, pluginContext.groupId)
                                   .tag(Tags.Messaging.KAFKA_CLIENT_ID, pluginContext.clientId)
                                   .start());
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ConsumerRecords<?, ?> records = aopContext.getReturningAs();
        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            span.tag(aopContext.getException())
                .tag(Tags.Messaging.COUNT, records == null ? 0 : records.count())
                .finish();
        }

        InterceptorContext.remove("kafka.consumer.context");
    }
}
