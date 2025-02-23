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

package org.bithon.agent.plugin.redis.lettuce.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 1/5/24 11:11 am
 */
public class Command$Execute extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ConnectionContext connectionContext = (ConnectionContext) ((IBithonObject) aopContext.getTargetAs()).getInjectedObject();
        if (connectionContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        // Use to UpperCase to comply with the name in Lettuce's AsyncCommand
        String operation = aopContext.getMethod().toUpperCase(Locale.ENGLISH);
        ITraceSpan span = TraceContextFactory.newSpan("lettuce");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .kind(SpanKind.CLIENT)
                               .tag(Tags.Net.PEER, connectionContext.endpoint)
                               .tag(Tags.Database.SYSTEM, "redis")
                               .tag(Tags.Database.REDIS_DB_INDEX, connectionContext.dbIndex)
                               .tag(Tags.Database.OPERATION, operation)
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException()).finish();
    }
}

