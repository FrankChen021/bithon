/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.internal.connection.CommandProtocol;
import com.mongodb.internal.connection.DefaultServerConnection;
import com.mongodb.internal.connection.LegacyProtocol;
import com.mongodb.session.SessionContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.mongo.MongoCommand;
import org.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

/**
 * @author frankchen
 */
public class DefaultServerConnectionExecuteProtocol extends AbstractInterceptor {
    static ILogAdaptor log = LoggerFactory.getLogger(DefaultServerConnectionExecuteProtocol.class);

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
        aopContext.setUserContext(TraceSpanFactory.newSpan("mongodb")
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
        ((ITraceSpan) aopContext.castUserContextAs())
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
