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

import com.mongodb.internal.connection.InternalConnection;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.BeforeInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.mongo.MongoDbMetricRegistry;

/**
 * {@link com.mongodb.internal.connection.CommandProtocolImpl#execute(InternalConnection)}
 */
public class CommandProtocolImpl$Execute extends BeforeInterceptor {

    private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

    /**
     * set command so that
     * {@link InternalStreamConnection$SendMessage}
     * {@link InternalStreamConnection$ReceiveMessage} know which command is being executed
     */
    @Override
    public void before(AopContext aopContext) throws Exception {
        IBithonObject bithonObject = aopContext.getTargetAs();

        InterceptorContext.set("mongo-3.8-command", bithonObject.getInjectedObject());
    }
}
