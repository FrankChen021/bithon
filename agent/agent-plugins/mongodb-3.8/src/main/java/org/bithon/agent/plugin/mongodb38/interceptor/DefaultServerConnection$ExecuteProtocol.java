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
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.metric.domain.mongo.MongoCommand;
import org.bithon.agent.observability.metric.domain.mongo.MongoDbMetricRegistry;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

/**
 * @author frankchen
 */
public class DefaultServerConnection$ExecuteProtocol extends AroundInterceptor {
    static ILogAdaptor log = LoggerFactory.getLogger(DefaultServerConnection$ExecuteProtocol.class);

    private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        // create a span and save it in user-context
        ITraceSpan span = TraceContextFactory.newSpan("mongodb");
        if (span != null) {
            aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                                   .kind(SpanKind.CLIENT)
                                   .start());
        }

        return InterceptionDecision.CONTINUE;
    }

    /**
     * {@link DefaultServerConnection#executeProtocol(LegacyProtocol)}
     * {@link DefaultServerConnection#executeProtocol(CommandProtocol, SessionContext)}
     */
    @Override
    public void after(AopContext aopContext) {
        DefaultServerConnection connection = aopContext.getTargetAs();
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
        ITraceSpan span = aopContext.getSpan();
        if (span != null) {
            span.tag(aopContext.getException())
                .tag(Tags.Net.PEER, hostAndPort)
                .tag(Tags.Database.NAME, command == null ? null : command.getDatabase())
                .tag(Tags.Database.MONGODB_DB_COLLECTION, command == null ? null : command.getCollection())
                .tag(Tags.Database.MONGODB_DB_COMMAND, command == null ? null : command.getCommand())
                .finish();
        }

        //
        // metric
        //
        if (command != null) {
            int exceptionCount = aopContext.hasException() ? 0 : 1;
            metricRegistry.getOrCreateMetric(hostAndPort,
                                             command.getDatabase(),
                                             command.getCollection(),
                                             command.getCommand())
                          .add(aopContext.getExecutionTime(), exceptionCount);
        }
    }
}
