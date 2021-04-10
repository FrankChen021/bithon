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

package com.sbss.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.connection.ConnectionId;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoCommand;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 * @date 2021-03-27 16:30
 */
public class ConnectionMessagesSentEvent {
    private static final Logger log = LoggerFactory.getLogger(ConnectionMessagesSentEvent.class);

    public static class Constructor extends AbstractInterceptor {
        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.x-metrics", MongoDbMetricCollector.class);
            return true;
        }

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

            metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString(),
                                              mongoCommand.getDatabase())
                           .addBytesOut(bytesOut);
        }
    }
}
