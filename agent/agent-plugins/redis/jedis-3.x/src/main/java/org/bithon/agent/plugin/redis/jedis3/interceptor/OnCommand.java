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

package org.bithon.agent.plugin.redis.jedis3.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.redis.RedisMetricRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/22 5:52 PM
 */
public class OnCommand extends AroundInterceptor {

    private final RedisMetricRegistry metricRegistry = RedisMetricRegistry.get();

    private final Set<String> ignoreCommands = new HashSet<>(Arrays.asList("AUTH",
                                                                           "Protocol.Command.SELECT",
                                                                           "Protocol.Command.ECHO",
                                                                           "Protocol.Command.QUIT"));

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        Jedis jedis = aopContext.getTargetAs();
        String hostAndPort = jedis.getClient().getHost() + ":" + jedis.getClient().getPort();
        long db = jedis.getClient().getDB();

        String command = aopContext.getMethod().toUpperCase(Locale.ENGLISH);

        // Keep the metrics in current thread local so that Input and Output stream can access them
        InterceptorContext.set("redis-command", new JedisContext(metricRegistry.getOrCreateMetrics(hostAndPort, command)));

        if (!ignoreCommands.contains(command)) {
            ITraceSpan span = TraceContextFactory.newSpan("jedis");
            if (span != null) {
                aopContext.setSpan(span.method(aopContext.getTargetClass().getName(), command)
                                       .kind(SpanKind.CLIENT)
                                       .tag(Tags.Net.PEER, hostAndPort)
                                       .tag(Tags.Database.REDIS_DB_INDEX, db)
                                       .tag(Tags.Database.SYSTEM, "redis")
                                       .start());
            }
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            span.tag(aopContext.getException()).finish();
        }

        JedisContext ctx = InterceptorContext.getAs("redis-command");
        if (ctx != null) {
            ctx.getMetrics().addRequest(aopContext.getExecutionTime(), aopContext.hasException() ? 1 : 0);
        }
        InterceptorContext.remove("redis-command");
    }
}
