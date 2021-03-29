package com.sbss.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.connection.ConnectionId;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class ConnectionMessagesSentEvent {
    private static final Logger log = LoggerFactory.getLogger(ConnectionMessagesSentEvent.class);

    public static class Constructor extends AbstractInterceptor {
        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
            return true;
        }

        /**
         * @param args final ConnectionId connectionId
         *             final int requestId
         *             final int size
         */
        @Override
        public void onConstruct(Object constructedObject, Object[] args) {
            MongoCommand mongoCommand = InterceptorContext.getAs("mongo-3.x-command");
            if (mongoCommand == null) {
                log.warn("Don' worry, the stack is dumped to help analyze the problem. No real exception happened.",
                         new RuntimeException());
                return;
            }

            ConnectionId connectionId = (ConnectionId) args[0];
            int bytesOut = (int) args[2];

            metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString(),
                                              mongoCommand.getDatabase(),
                                              mongoCommand.getCollection(),
                                              mongoCommand.getCommand())
                           .addBytesOut(bytesOut);
        }
    }
}