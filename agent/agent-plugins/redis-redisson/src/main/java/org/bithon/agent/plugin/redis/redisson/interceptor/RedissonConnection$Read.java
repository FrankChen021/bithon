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

package org.bithon.agent.plugin.redis.redisson.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.redis.RedisMetricRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.plugin.redis.redisson.ConnectionContext;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;

/**
 * {@link org.redisson.spring.data.connection.RedissonConnection#read(byte[], Codec, RedisCommand, Object...)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/4 22:58
 */
public class RedissonConnection$Read extends AroundInterceptor {
    private final RedisMetricRegistry metricRegistry = RedisMetricRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        RedisCommand<?> redisCommand = aopContext.getArgAs(2);
        String operation = redisCommand.getName();

        ITraceSpan span = TraceContextFactory.newSpan("spring-redisson");
        if (span != null) {
            aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                   .kind(SpanKind.CLIENT)
                                   .tag(Tags.Database.SYSTEM, "redis")
                                   .tag(Tags.Database.OPERATION, operation)
                                   .start());
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        RedisCommand<?> redisCommand = aopContext.getArgAs(2);
        ConnectionContext connectionContext = (ConnectionContext) ((IBithonObject) redisCommand).getInjectedObject();

        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            span.tag(aopContext.getException())
                .tag(Tags.Net.PEER, connectionContext == null ? null : connectionContext.endpoint)
                .tag(Tags.Database.REDIS_DB_INDEX, connectionContext == null ? null : connectionContext.dbIndex)
                .finish();
        }

        if (connectionContext != null) {
            metricRegistry.getOrCreateMetrics(connectionContext.endpoint, redisCommand.getName())
                          .addRequest(aopContext.getExecutionTime(), aopContext.hasException() ? 1 : 0)
                          .addRequestBytes(connectionContext.requestBytes)
                          .addResponseBytes(connectionContext.responseBytes);
        }
    }
}
