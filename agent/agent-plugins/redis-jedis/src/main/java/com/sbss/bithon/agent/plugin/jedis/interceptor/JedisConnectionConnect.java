package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisMetricCollector;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
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
