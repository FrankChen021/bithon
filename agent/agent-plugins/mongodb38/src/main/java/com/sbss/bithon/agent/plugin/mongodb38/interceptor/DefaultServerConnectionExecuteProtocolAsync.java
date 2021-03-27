package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.internal.connection.CommandProtocol;
import com.mongodb.internal.connection.DefaultServerConnection;
import com.mongodb.internal.connection.LegacyProtocol;
import com.mongodb.session.SessionContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;

/**
 * @author frankchen
 */
public class DefaultServerConnectionExecuteProtocolAsync extends AbstractInterceptor {
    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);
        return true;
    }

    /**
     * {@link DefaultServerConnection#executeProtocolAsync(LegacyProtocol, SingleResultCallback)}
     * {@link DefaultServerConnection#executeProtocolAsync(CommandProtocol, SessionContext, SingleResultCallback)}
     */
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        // TODO: WRAP callback
        return super.onMethodEnter(aopContext);
    }
}
