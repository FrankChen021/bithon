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
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.redis.RedisMetricCollector;
import org.bithon.agent.core.utils.ReflectionUtils;
import redis.clients.jedis.Connection;

/**
 * @author frankchen
 */
public class JedisConnectionConnect extends AbstractInterceptor {
    private RedisMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("jedis", RedisMetricCollector.class);
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Connection connection = aopContext.castTargetAs();
        return connection.isConnected() ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        Connection connection = aopContext.castTargetAs();
        String hostAndPort = connection.getHost() + ":" + connection.getPort();

        // bind input stream and output stream to corresponding connection object
        IBithonObject inputStream = (IBithonObject) ReflectionUtils.getFieldValue(connection, "inputStream");
        IBithonObject outputStream = (IBithonObject) ReflectionUtils.getFieldValue(connection, "outputStream");
        inputStream.setInjectedObject(hostAndPort);
        outputStream.setInjectedObject(hostAndPort);
    }
}
