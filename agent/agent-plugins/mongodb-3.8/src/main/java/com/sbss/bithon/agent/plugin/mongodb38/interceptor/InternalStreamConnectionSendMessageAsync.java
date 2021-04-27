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

import com.mongodb.connection.ConnectionId;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import com.sbss.bithon.agent.plugin.mongodb38.MetricHelper;
import org.bson.ByteBuf;

import java.util.List;

/**
 * @author frankchen
 */
public class InternalStreamConnectionSendMessageAsync extends AbstractInterceptor {

    private MongoDbMetricCollector metricCollector;

    @Override
    public boolean initialize() throws Exception {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("mongodb-3.8-metrics", MongoDbMetricCollector.class);

        return super.initialize();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onMethodLeave(AopContext aopContext) {
        InternalStreamConnection target = (InternalStreamConnection) aopContext.getTarget();

        List<ByteBuf> byteBufList = (List<ByteBuf>) aopContext.getArgs()[0];
        ConnectionId connectionId = target.getDescription().getConnectionId();
        int bytesOut = MetricHelper.getMessageSize(byteBufList);

        metricCollector.getOrCreateMetric(connectionId.getServerId().getAddress().toString())
                       .addBytesOut(bytesOut);
    }
}
