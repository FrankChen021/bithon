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

import com.mongodb.MongoNamespace;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.mongo.MongoCommand;
import org.bithon.agent.core.metric.domain.mongo.MongoDbMetricRegistry;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/29 1:53 下午
 */
public class CommandHelper {

    /**
     * see com.mongodb.connection.CommandHelper#executeCommand(String, org.bson.BsonDocument, com.mongodb.connection.InternalConnection)
     */
    public static class ExecuteCommand extends AbstractInterceptor {

        private final Map<String, Method> methods = new ConcurrentHashMap<>();

        private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

        @Override
        public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
            //
            // set command to thread context so that the size of sent/received could be associated with the command
            //
            MongoCommand command = new MongoCommand((String) aopContext.getArgs()[0],
                                                    MongoNamespace.COMMAND_COLLECTION_NAME,
                                                    //TODO: extract command from 2nd parameter
                                                    "Command");
            InterceptorContext.set("mongo-3.x-command", command);

            return super.onMethodEnter(aopContext);
        }

        /**
         * see {@link InternalStreamConnection}
         * the 3rd argument is an instance of subclass of {@link com.mongodb.connection.InternalConnection}, which is not visible from outside jar
         * So, it's constructor is intercepted to inject server address. By doing that, the following method could get that info from injected info.
         * <p>
         * We could also get the server information by Reflection, which is a little bit more overhead.
         * Although the overhead is acceptable since the CPM is not high
         */
        @Override
        public void onMethodLeave(AopContext aopContext) throws Exception {
            Object connection = aopContext.getArgs()[2];
            if (!(connection instanceof IBithonObject)) {
                //TODO: log
                return;
            }

            String server = (String) ((IBithonObject) connection).getInjectedObject();
            metricRegistry.getOrCreateMetric(server, (String) aopContext.getArgs()[0])
                          .add(aopContext.getCostTime(), aopContext.hasException() ? 1 : 0);
            super.onMethodLeave(aopContext);
        }
    }

    /**
     * {@link com.mongodb.connection.CommandHelper#executeCommandAsync(String, BsonDocument, com.mongodb.connection.InternalConnection)}
     */
    public static class ExecuteCommandAsync extends AbstractInterceptor {
        @Override
        public void onMethodLeave(AopContext aopContext) throws Exception {
            super.onMethodLeave(aopContext);
        }
    }
}
