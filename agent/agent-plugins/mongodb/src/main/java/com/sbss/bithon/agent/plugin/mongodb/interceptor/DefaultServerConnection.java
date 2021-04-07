package com.sbss.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.connection.Connection;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoCommand;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import com.sbss.bithon.agent.core.tracing.context.TraceSpanBuilder;
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
    public static class ExecuteProtocol extends AbstractInterceptor {
        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
            return true;
        }

        @Override
        public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
            //
            // set command to thread context so that the size of sent/received could be associated with the command
            //
            Object protocol = aopContext.getArgs()[0];
            if ((protocol instanceof IBithonObject)) {
                IBithonObject bithonObject = (IBithonObject) protocol;
                MongoCommand command = (MongoCommand) bithonObject.getInjectedObject();

                InterceptorContext.set("mongo-3.x-command", command);
            } else {
                InterceptorContext.set("mongo-3.x-command", null);
            }

            // create a span and save it in user-context
            aopContext.setUserContext(TraceSpanBuilder.build("mongodb")
                                                      .clazz(aopContext.getTargetClass())
                                                      .method(aopContext.getMethod())
                                                      .kind(SpanKind.CLIENT)
                                                      .start());

            return InterceptionDecision.CONTINUE;
        }

        @Override
        public void onMethodLeave(AopContext aopContext) {
            Connection connection = aopContext.castTargetAs();
            String hostAndPort = connection.getDescription().getServerAddress().toString();

            MongoCommand command = null;
            Object protocol = aopContext.getArgs()[0];
            if ((protocol instanceof IBithonObject)) {
                IBithonObject bithonObject = (IBithonObject) protocol;
                command = (MongoCommand) bithonObject.getInjectedObject();
            } else {
                log.warn("No worry. This exception is printed only for problem addressing. No Real exception happens.",
                         new RuntimeException());
            }

            //
            // trace
            //
            ((TraceSpan) aopContext.castUserContextAs())
                .tag(aopContext.getException())
                .tag("server", hostAndPort)
                .tag("database", command == null ? null : command.getDatabase())
                .finish();


            //
            // metric
            //
            if (command != null) {
                int exceptionCount = aopContext.hasException() ? 0 : 1;
                metricCollector.getOrCreateMetric(hostAndPort, command.getDatabase())
                               .add(aopContext.getCostTime(), exceptionCount);
            }
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