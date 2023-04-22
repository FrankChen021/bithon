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

package org.bithon.agent.plugin.apache.kafka.producer.interceptor;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceSpanFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.producer.KafkaProducerTracingConfig;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author Frank Chen
 * @date 18/11/22 10:33 am
 */
public class KafkaProducer$DoSend extends AroundInterceptor {

    private final KafkaProducerTracingConfig tracingConfig = ConfigurationManager.getInstance().getConfig(KafkaProducerTracingConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("kafka");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ProducerRecord<?, ?> record = aopContext.getArgAs(0);

        // record headers
        for (String key : tracingConfig.getHeaders()) {
            Header header = record.headers().lastHeader(key);
            if (header != null) {
                span.tag("kafka.header." + key, new String(header.value(), StandardCharsets.UTF_8));
            }
        }

        // message size
        Integer size = null;
        if (record.value() instanceof String) {
            size = ((String) record.value()).length();
        } else if (record.value() instanceof byte[]) {
            size = ((byte[]) record.value()).length;
        } else if (record.value() instanceof ByteBuffer) {
            size = ((ByteBuffer) record.value()).remaining();
        }

        String cluster = ((KafkaPluginContext) ((IBithonObject) aopContext.getTarget()).getInjectedObject()).clusterSupplier.get();

        aopContext.setUserContext(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                      .kind(SpanKind.PRODUCER)
                                      .tag("uri", "kafka://" + cluster)
                                      .tag("kafka.topic", record.topic())
                                      .tag("kafka.partition", record.partition())
                                      .tag("kafka.messageSize", size)
                                      .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getUserContextAs();
        if (aopContext.hasException()) {
            span.tag(aopContext.getException());
        } else {
            Object returning = aopContext.getReturning();
            if (returning != null) {
                if ("org.apache.kafka.clients.producer.KafkaProducer$FutureFailure".equals(returning.getClass().getName())) {
                    Exception exception = (Exception) ReflectionUtils.getFieldValue(returning, "exception");
                    if (exception != null) {
                        span.tag(exception.getCause());
                    }
                }
            }
        }
        span.finish();
    }
}
