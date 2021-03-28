package com.sbss.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.connection.Connection;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class DefaultServerConnection {
    static Logger log = LoggerFactory.getLogger(DefaultServerConnection.class);

    /**
     * {@link com.mongodb.connection.DefaultServerConnection#executeProtocol(com.mongodb.connection.Protocol)}
     */
    public abstract static class ExecuteProtocol extends AbstractInterceptor {
        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
            return true;
        }

        @Override
        public void onMethodLeave(AopContext aopContext) {
            Object protocol = aopContext.getArgs()[0];
            if (!(protocol instanceof IBithonObject)) {
                log.warn("Unknown Command", new RuntimeException());
                return;
            }

            IBithonObject bithonObject = (IBithonObject) protocol;
            MongoCommand command = (MongoCommand) bithonObject.getInjectedObject();

            Connection connection = aopContext.castTargetAs();
            String hostAndPort = connection.getDescription().getServerAddress().toString();

            int exceptionCount = aopContext.hasException() ? 0 : 1;

            metricCollector.getOrCreateMetric(hostAndPort,
                                              command.getDatabase(),
                                              command.getCollection(),
                                              command.getCommand())
                           .add(aopContext.getCostTime(), exceptionCount);

        }
    }

    /**
     * {@link com.mongodb.connection.DefaultServerConnection#executeProtocolAsync(com.mongodb.connection.Protocol, SingleResultCallback)}
     */
    public static class ExecuteProtocolAsync extends AbstractInterceptor {
        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
            return true;
        }

        /**
         * {@link com.mongodb.connection.DefaultServerConnection#executeProtocolAsync(com.mongodb.connection.Protocol, com.mongodb.async.SingleResultCallback)}
         */
        @Override
        public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
            Object protocol = aopContext.getArgs()[0];
            if (!(protocol instanceof IBithonObject)) {
                log.warn("Unknown Command", new RuntimeException());
                return InterceptionDecision.SKIP_LEAVE;
            }

            IBithonObject bithonObject = (IBithonObject) protocol;
            MongoCommand command = (MongoCommand) bithonObject.getInjectedObject();

            //TODO: wrap callback and exception callback
            SingleResultCallback callback = (SingleResultCallback) aopContext.getArgs()[1];

            return super.onMethodEnter(aopContext);
        }
    }
}