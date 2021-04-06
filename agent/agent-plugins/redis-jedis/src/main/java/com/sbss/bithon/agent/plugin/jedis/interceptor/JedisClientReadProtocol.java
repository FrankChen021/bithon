package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisMetricCollector;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import redis.clients.jedis.Client;

/**
 * @author frankchen
 */
public class JedisClientReadProtocol extends AbstractInterceptor {
    private RedisMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("jedis", RedisMetricCollector.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Client redisClient = aopContext.castTargetAs();

        //TODO: cache the name in Client object
        String endpoint = redisClient.getHost() + ":" + redisClient.getPort();

        String command = InterceptorContext.getAs("redis-command");
        metricCollector.addRead(endpoint,
                                command,
                                aopContext.getCostTime(),
                                aopContext.hasException());
    }
}
