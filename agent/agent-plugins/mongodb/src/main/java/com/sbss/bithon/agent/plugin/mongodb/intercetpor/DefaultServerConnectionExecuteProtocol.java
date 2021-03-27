package com.sbss.bithon.agent.plugin.mongodb.intercetpor;

import com.mongodb.connection.Connection;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.plugin.mongodb.MongoDbMetricCollector;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class DefaultServerConnectionExecuteProtocol extends AbstractInterceptor {
    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
        return true;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Connection connection = (Connection) aopContext.getTarget();
        String hostAndPort = connection.getDescription().getServerAddress().toString();

        int exceptionCount = aopContext.hasException() ? 0 : 1;

        metricCollector.getOrCreateMetric(hostAndPort)
                       .add(aopContext.getCostTime(), exceptionCount);
    }
}
