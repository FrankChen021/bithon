package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;

/**
 * @author frankchen
 */
public class InternalStreamConnectionReceiveMessageAsync extends AbstractInterceptor {

    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() throws Exception {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);

        return super.initialize();
    }

    /**
     * interceptor of {@link InternalStreamConnection#receiveMessageAsync(int, SingleResultCallback)}
     */
    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {

        //TODO: install wrapper for callback
        return super.onMethodEnter(aopContext);
    }
}
