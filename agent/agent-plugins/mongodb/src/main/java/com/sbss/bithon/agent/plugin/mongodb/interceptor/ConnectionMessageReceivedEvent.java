package com.sbss.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.connection.ConnectionId;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoCommand;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class ConnectionMessageReceivedEvent {
    private static final Logger log = LoggerFactory.getLogger(ConnectionMessageReceivedEvent.class);

    /**
     * {@link com.mongodb.event.ConnectionMessagesSentEvent#ConnectionMessagesSentEvent(ConnectionId, int, int size)}
     */
    public static class Constructor extends AbstractInterceptor {
        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
            return true;
        }

        @Override
        public void onConstruct(AopContext aopContext) {
            MongoCommand mongoCommand = InterceptorContext.getAs("mongo-3.x-command");
            if ( mongoCommand == null ) {
                log.warn("Don' worry, the stack is dumped to help analyze the problem. No real exception happened.", new RuntimeException());
                return;
            }

            ConnectionId connectionId = aopContext.getArgAs(0);
            int bytesIn = aopContext.getArgAs(2);

            // TODO: if there's no protocol is being executed, are there messages ?
            metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString(),
                                              mongoCommand.getDatabase())
                           .addBytesIn(bytesIn);
        }
    }
}