/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.internal.connection.CommandProtocol;
import com.mongodb.internal.connection.DefaultServerConnection;
import com.mongodb.internal.connection.LegacyProtocol;
import com.mongodb.session.SessionContext;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
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
 */
public class DefaultServerConnectionExecuteProtocol extends AbstractInterceptor {
    static Logger log = LoggerFactory.getLogger(DefaultServerConnectionExecuteProtocol.class);

    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);
        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        // create a span and save it in user-context
        aopContext.setUserContext(TraceSpanBuilder.build("mongodb")
                                                  .clazz(aopContext.getTargetClass())
                                                  .method(aopContext.getMethod())
                                                  .kind(SpanKind.CLIENT)
                                                  .start());

        return InterceptionDecision.CONTINUE;
    }

    /**
     * {@link DefaultServerConnection#executeProtocol(LegacyProtocol)}
     * {@link DefaultServerConnection#executeProtocol(CommandProtocol, SessionContext)}
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        DefaultServerConnection connection = aopContext.castTargetAs();
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
