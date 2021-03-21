package com.sbss.bithon.agent.plugin.mongodb;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;

/**
 * @author frankchen
 */
public class MongoDbHandler extends AbstractInterceptor {
    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MongoDbMetricCollector.getInstance();
        return true;
    }

//    @Override
//    public void onConstruct(Object constructedObject,
//                            Object[] args) {
//        AopContext joinPoint = new AopContext(constructedObject, null, args, null, null, null);
//        counter.update(joinPoint);
//    }

    @Override
    public void onMethodLeave(AopContext context) {
        metricCollector.update(context);
    }
}
