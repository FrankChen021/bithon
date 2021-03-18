package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.MetricProviderManager;
import com.sbss.bithon.agent.core.metric.redis.RedisMetricProvider;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import redis.clients.jedis.Client;

/**
 * @author frankchen
 */
public class JedisClientSendCommand extends AbstractInterceptor {
    private RedisMetricProvider metricProvider;

    @Override
    public boolean initialize() {
        metricProvider = MetricProviderManager.getInstance().getOrRegister("jedis", RedisMetricProvider.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Client redisClient = aopContext.castTargetAs();
        String hostAndPort = redisClient.getHost() + ":" + redisClient.getPort();

        String command = aopContext.getArgs()[0].toString();
        InterceptorContext.set("redis-command", command);
        metricProvider.addWrite(hostAndPort,
                                command,
                                aopContext.getCostTime(),
                                aopContext.hasException());
    }
}
