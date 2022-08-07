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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.redis.RedisMetricRegistry;
import redis.clients.jedis.Jedis;

import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 22/1/22 5:52 PM
 */
public class OnCommand extends AbstractInterceptor {

    private final RedisMetricRegistry metricRegistry = RedisMetricRegistry.get();

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Jedis jedis = aopContext.castTargetAs();
        String hostAndPort = jedis.getClient().getHost() + ":" + jedis.getClient().getPort();
        //String db = jedis.getDB();

        String command = aopContext.getMethod().getName().toUpperCase(Locale.ENGLISH);

        InterceptorContext.set("redis-command", new JedisContext(metricRegistry.getOrCreateMetrics(hostAndPort, command)));

        return super.onMethodEnter(aopContext);
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        JedisContext ctx = (JedisContext) InterceptorContext.get("redis-command");
        if (ctx == null) {
            return;
        }

        ctx.getMetrics().addRequest(aopContext.getCostTime(), aopContext.hasException() ? 1 : 0);
    }
}
