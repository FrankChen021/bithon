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

package org.bithon.agent.plugin.apache.kafka.admin.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.component.commons.tracing.Components;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * @author Frank Chen
 * @date 2/1/24 11:47 am
 */
public class KafkaAdminClient$All extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan(Components.KAFKA);
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        IBithonObject bithonObject = aopContext.getTargetAs();
        KafkaPluginContext pluginContext = (KafkaPluginContext) bithonObject.getInjectedObject();

        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .kind(SpanKind.CLIENT)
                               .tag(Tags.Messaging.SYSTEM, "kafka")
                               .tag(Tags.Net.PEER, pluginContext.clusterSupplier.get())
                               .tag(Tags.Messaging.KAFKA_CLIENT_ID, pluginContext.clientId)
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException()).finish();
    }
}
