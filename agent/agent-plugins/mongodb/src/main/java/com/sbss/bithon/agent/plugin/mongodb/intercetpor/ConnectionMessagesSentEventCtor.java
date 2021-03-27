package com.sbss.bithon.agent.plugin.mongodb.intercetpor;

import com.mongodb.connection.ConnectionId;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class ConnectionMessagesSentEventCtor extends AbstractInterceptor {
    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
        return true;
    }

    /**
     * @param args              final ConnectionId connectionId
     *                          final int requestId
     *                          final int size
     */
    @Override
    public void onConstruct(Object constructedObject, Object[] args) {
        ConnectionId connectionId = (ConnectionId) args[0];
        int bytesOut = (int) args[2];

        metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString())
                       .addBytesOut(bytesOut);
    }
}
