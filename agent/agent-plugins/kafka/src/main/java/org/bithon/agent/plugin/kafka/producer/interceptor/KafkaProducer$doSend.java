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

package org.bithon.agent.plugin.kafka.producer.interceptor;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.tracing.SpanKind;

/**
 * @author Frank Chen
 * @date 18/11/22 10:33 am
 */
public class KafkaProducer$doSend extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        ITraceSpan span = TraceSpanFactory.newSpan("kafka");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ProducerRecord<?, ?> record = aopContext.getArgAs(0);
        Integer size = null;
        if (record.value() instanceof String) {
            size = ((String) record.value()).length();
        }
        aopContext.setUserContext(span.method(aopContext.getMethod())
                                      .kind(SpanKind.PRODUCER)
                                      .tag("kafka.topic", record.topic())
                                      .tag("kafka.partition", record.partition())
                                      .tag("kafka.messageSize", size)
                                      .start());


        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        ITraceSpan span = aopContext.castUserContextAs();
        span.tag(aopContext.getException()).finish();
    }
}
