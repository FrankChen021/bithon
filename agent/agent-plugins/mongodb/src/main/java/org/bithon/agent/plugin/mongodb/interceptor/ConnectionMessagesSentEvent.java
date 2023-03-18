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

package org.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.connection.ConnectionId;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.mongo.MongoCommand;
import org.bithon.agent.observability.metric.domain.mongo.MongoDbMetricRegistry;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class ConnectionMessagesSentEvent {
    private static final ILogAdaptor log = LoggerFactory.getLogger(ConnectionMessagesSentEvent.class);

    public static class Constructor extends AbstractInterceptor {
        private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

        @Override
        public void onConstruct(AopContext aopContext) {
            MongoCommand mongoCommand = InterceptorContext.getAs("mongo-3.x-command");
            if (mongoCommand == null) {
                log.warn("Don' worry, the stack is dumped to help analyze the problem. No real exception happened.",
                         new RuntimeException());
                return;
            }

            ConnectionId connectionId = aopContext.getArgAs(0);
            int bytesOut = aopContext.getArgAs(2);

            metricRegistry.getOrCreateMetric(connectionId.getServerId().getAddress().toString(),
                                             mongoCommand.getDatabase(),
                                             mongoCommand.getCollection(),
                                             mongoCommand.getCommand())
                          .addBytesOut(bytesOut);
        }
    }
}
