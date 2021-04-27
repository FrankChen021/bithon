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

import com.mongodb.MongoNamespace;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.internal.connection.ClusterClock;
import com.mongodb.internal.connection.InternalConnection;
import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoCommand;
import com.sbss.bithon.agent.core.metric.domain.mongo.MongoDbMetricCollector;
import org.bson.BsonDocument;

/**
 * CommandHelper is called before InternalStreamConnection#sendMessage
 * <p>
 * intercept related methods to set database for interceptors of sendMessage
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/29 1:53 下午
 */
public class CommandHelper {

    /**
     * {@link com.mongodb.internal.connection.CommandHelper#executeCommand(String, BsonDocument, InternalConnection)}
     * {@link com.mongodb.internal.connection.CommandHelper#executeCommand(String, BsonDocument, ClusterClock, InternalConnection)}
     */
    public static class ExecuteCommand extends AbstractInterceptor {

        private MongoDbMetricCollector metricCollector;

        @Override
        public boolean initialize() {
            metricCollector = MetricCollectorManager.getInstance()
                                                    .getOrRegister("mongo-3.8-metrics", MongoDbMetricCollector.class);
            return true;
        }

        @Override
        public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
            //
            // set command to thread context so that the size of sent/received could be associated with the command
            //
            MongoCommand command = new MongoCommand((String) aopContext.getArgs()[0],
                                                    MongoNamespace.COMMAND_COLLECTION_NAME,
                                                    //TODO: extract command from 2nd parameter
                                                    "Command");
            InterceptorContext.set("mongo-3.8-command", command);

            return super.onMethodEnter(aopContext);
        }

        @Override
        public void onMethodLeave(AopContext aopContext) throws Exception {
            InternalConnection connection = aopContext.getArgs().length > 3
                                            ? aopContext.getArgAs(3)
                                            : aopContext.getArgAs(2);

            String server = connection.getDescription().getServerAddress().toString();
            metricCollector.getOrCreateMetric(server, (String) aopContext.getArgs()[0])
                           .add(aopContext.getCostTime(), aopContext.hasException() ? 1 : 0);
            super.onMethodLeave(aopContext);
        }
    }

    /**
     * TODO:
     * {@link com.mongodb.internal.connection.CommandHelper#executeCommandAsync(String, BsonDocument, InternalConnection, SingleResultCallback)}
     */
    public static class ExecuteCommandAsync extends AbstractInterceptor {
        @Override
        public void onMethodLeave(AopContext aopContext) throws Exception {
            super.onMethodLeave(aopContext);
        }
    }
}
