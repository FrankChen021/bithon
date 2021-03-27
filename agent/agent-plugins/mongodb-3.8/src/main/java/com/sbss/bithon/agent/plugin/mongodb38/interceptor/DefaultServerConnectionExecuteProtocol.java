package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.internal.connection.CommandProtocol;
import com.mongodb.internal.connection.DefaultServerConnection;
import com.mongodb.internal.connection.LegacyProtocol;
import com.mongodb.session.SessionContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class DefaultServerConnectionExecuteProtocol extends AbstractInterceptor {
    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);
        return true;
    }

    /**
     * {@link DefaultServerConnection#executeProtocol(LegacyProtocol)}
     * {@link DefaultServerConnection#executeProtocol(CommandProtocol, SessionContext)}
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        DefaultServerConnection connection = aopContext.castTargetAs();
        String hostAndPort = connection.getDescription().getServerAddress().toString();
        int exceptionCount = aopContext.hasException() ? 0 : 1;


        metricCollector.getOrCreateMetric(hostAndPort)
                       .add(aopContext.getCostTime(), exceptionCount);
    }
}
