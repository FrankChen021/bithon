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
import com.mongodb.async.SingleResultCallback;
import com.mongodb.internal.connection.InternalConnection;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.domain.mongo.MongoCommand;
import org.bithon.agent.core.metric.domain.mongo.MongoDbMetricRegistry;

/**
 * CommandHelper is called before InternalStreamConnection#sendMessage
 * <p>
 * intercept related methods to set database for interceptors of sendMessage
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/29 1:53 下午
 */
public class CommandProtocolImpl {

    /**
     * {@link com.mongodb.internal.connection.CommandProtocolImpl}
     */
    public static class Constructor extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            IBithonObject obj = aopContext.castTargetAs();
            obj.setInjectedObject(new MongoCommand(aopContext.getArgAs(0),
                                                   MongoNamespace.COMMAND_COLLECTION_NAME,
                                                   "Command"));
        }
    }

    /**
     * {@link com.mongodb.internal.connection.CommandProtocolImpl#execute(InternalConnection)}
     */
    public static class Execute extends AbstractInterceptor {

        private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

        /**
         * set command so that
         * {@link InternalStreamConnection$SendMessage}
         * {@link InternalStreamConnection$ReceiveMessage} know which command is being executed
         */
        @Override
        public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
            IBithonObject bithonObject = aopContext.castTargetAs();

            InterceptorContext.set("mongo-3.8-command", bithonObject.getInjectedObject());

            return super.onMethodEnter(aopContext);
        }
    }

    /**
     * TODO:
     * {@link com.mongodb.internal.connection.CommandProtocolImpl#executeAsync(InternalConnection, SingleResultCallback)}
     */
    public static class ExecuteAsync extends AbstractInterceptor {
        @Override
        public void onMethodLeave(AopContext aopContext) throws Exception {
            super.onMethodLeave(aopContext);
        }
    }
}
