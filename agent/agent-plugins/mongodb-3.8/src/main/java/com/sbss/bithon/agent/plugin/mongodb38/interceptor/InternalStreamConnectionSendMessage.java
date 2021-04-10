package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.connection.ConnectionId;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoCommand;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.plugin.mongodb38.MetricHelper;
import org.bson.ByteBuf;

import java.util.List;

/**
 * @author frankchen
 */
public class InternalStreamConnectionSendMessage extends AbstractInterceptor {

    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() throws Exception {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);

        return super.initialize();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMethodLeave(AopContext aopContext) {
        MongoCommand command = InterceptorContext.getAs("mongo-3.8-command");

        InternalStreamConnection target = (InternalStreamConnection) aopContext.getTarget();

        List<ByteBuf> byteBufList = (List<ByteBuf>) aopContext.getArgs()[0];
        ConnectionId connectionId = target.getDescription().getConnectionId();
        int bytesOut = MetricHelper.getMessageSize(byteBufList);

        metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString(),
                                          command.getDatabase())
                       .addBytesOut(bytesOut);
    }
}
