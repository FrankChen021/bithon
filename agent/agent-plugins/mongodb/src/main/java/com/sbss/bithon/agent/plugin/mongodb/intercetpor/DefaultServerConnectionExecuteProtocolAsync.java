package com.sbss.bithon.agent.plugin.mongodb.intercetpor;

import com.mongodb.async.SingleResultCallback;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.plugin.mongodb.MongoDbMetricCollector;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class DefaultServerConnectionExecuteProtocolAsync extends AbstractInterceptor {
    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        //TODO: wrap callback and exception callback
        com.mongodb.async.SingleResultCallback callback = (SingleResultCallback) aopContext.getArgs()[1];

        return super.onMethodEnter(aopContext);
    }
}
