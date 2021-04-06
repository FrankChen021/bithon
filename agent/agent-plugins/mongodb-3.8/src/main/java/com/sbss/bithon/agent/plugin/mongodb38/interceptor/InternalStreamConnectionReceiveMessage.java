package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.connection.ConnectionId;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.mongodb.internal.connection.ResponseBuffers;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;

/**
 * @author frankchen
 */
public class InternalStreamConnectionReceiveMessage extends AbstractInterceptor {

    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() throws Exception {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);

        return super.initialize();
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        InternalStreamConnection target = (InternalStreamConnection) aopContext.getTarget();
        ConnectionId connectionId = target.getDescription().getConnectionId();

        ResponseBuffers result = aopContext.castReturningAs();
        int bytesIn = result.getReplyHeader().getMessageLength();
        bytesIn += result.getBodyByteBuffer().remaining();

        metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString())
                       .addBytesIn(bytesIn);
    }
}
