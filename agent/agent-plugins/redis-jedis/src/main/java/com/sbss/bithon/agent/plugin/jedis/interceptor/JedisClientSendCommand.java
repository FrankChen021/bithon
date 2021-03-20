package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.redis.RedisMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import redis.clients.jedis.Client;

/**
 * @author frankchen
 */
public class JedisClientSendCommand extends AbstractInterceptor {
    private RedisMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("jedis", RedisMetricCollector.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Client redisClient = aopContext.castTargetAs();
        String hostAndPort = redisClient.getHost() + ":" + redisClient.getPort();

        String command = aopContext.getArgs()[0].toString();
        InterceptorContext.set("redis-command", command);
        metricCollector.addWrite(hostAndPort,
                                 command,
                                 aopContext.getCostTime(),
                                 aopContext.hasException());
    }
}
