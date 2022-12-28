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

import com.mongodb.MongoNamespace;
import com.mongodb.internal.connection.ClusterClock;
import com.mongodb.internal.connection.InternalConnection;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.mongo.MongoCommand;
import org.bithon.agent.core.metric.domain.mongo.MongoDbMetricRegistry;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bson.BsonDocument;

/**
 * CommandHelper is called before InternalStreamConnection#sendMessage
 * <p>
 * intercept related methods to set database for interceptors of sendMessage
 * <p>
 * {@link com.mongodb.internal.connection.CommandHelper#executeCommand(String, BsonDocument, InternalConnection)}
 * {@link com.mongodb.internal.connection.CommandHelper#executeCommand(String, BsonDocument, ClusterClock, InternalConnection)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/29 1:53 下午
 */
public class CommandHelper$ExecuteCommand extends AbstractInterceptor {

    private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        int lastIndex = aopContext.getArgs().length - 1;
        if (lastIndex == -1) {
            return InterceptionDecision.SKIP_LEAVE;
        }
        if (!(aopContext.getArgs()[lastIndex] instanceof InternalConnection)) {
            LoggerFactory.getLogger(CommandHelper$ExecuteCommand.class)
                         .error("Interceptor does not work for {}. Maybe the target library version is not compatible with the agent. ",
                                aopContext.getMethod().toString());
            return InterceptionDecision.SKIP_LEAVE;
        }

        //
        // set command to thread context so that the size of sent/received could be associated with the command
        //
        MongoCommand command = new MongoCommand((String) aopContext.getArgs()[0],
                                                MongoNamespace.COMMAND_COLLECTION_NAME,
                                                //TODO: extract command from 2nd parameter
                                                "Command");
        InterceptorContext.set("mongo-3.8-command", command);
        aopContext.setUserContext(command);

        return super.onMethodEnter(aopContext);
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        int lastIndex = aopContext.getArgs().length - 1;
        InternalConnection connection = aopContext.getArgAs(lastIndex);
        String server = connection.getDescription().getServerAddress().toString();

        MongoCommand command = aopContext.getUserContextAs();
        metricRegistry.getOrCreateMetric(server,
                                         command.getDatabase(),
                                         command.getCollection(),
                                         command.getCommand())
                      .add(aopContext.getExecutionTime(), aopContext.hasException() ? 1 : 0);
        super.onMethodLeave(aopContext);
    }
}
