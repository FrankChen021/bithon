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

package org.bithon.agent.plugin.jedis.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.redis.RedisMetricRegistry;
import redis.clients.jedis.Jedis;

import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/22 5:52 PM
 */
public class OnCommand extends AroundInterceptor {

    private final RedisMetricRegistry metricRegistry = RedisMetricRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) throws Exception {
        Jedis jedis = aopContext.getTargetAs();
        String hostAndPort = jedis.getClient().getHost() + ":" + jedis.getClient().getPort();
        //String db = jedis.getDB();

        String command = aopContext.getMethod().getName().toUpperCase(Locale.ENGLISH);

        InterceptorContext.set("redis-command", new JedisContext(metricRegistry.getOrCreateMetrics(hostAndPort, command)));

        return super.before(aopContext);
    }

    @Override
    public void after(AopContext aopContext) throws Exception {
        JedisContext ctx = (JedisContext) InterceptorContext.get("redis-command");
        if (ctx == null) {
            return;
        }

        ctx.getMetrics().addRequest(aopContext.getExecutionTime(), aopContext.hasException() ? 1 : 0);
    }
}
