package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.metrics.redis.RedisMetricProvider;
import redis.clients.jedis.Client;

/**
 * @author frankchen
 */
public class JedisClientReadProtocol extends AbstractInterceptor {
    private RedisMetricProvider metricProvider;

    @Override
    public boolean initialize() {
        metricProvider = MetricProviderManager.getInstance().getOrRegister("jedis", RedisMetricProvider.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Client redisClient = aopContext.castTargetAs();

        //TODO: cache the name in Client object
        String hostAndPort = redisClient.getHost() + ":" + redisClient.getPort();

        String command = InterceptorContext.getAs("redis-command");
        metricProvider.addRead(hostAndPort,
                               command,
                               aopContext.getCostTime(),
                               aopContext.hasException());
    }
}
