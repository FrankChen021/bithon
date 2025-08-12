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


import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.logging.LoggerAdaptor;
import org.bithon.agent.observability.metric.domain.mongo.MongoDbMetricRegistry;

/**
 * {@link com.mongodb.connection.DefaultServerConnection#executeProtocolAsync(com.mongodb.connection.Protocol, SingleResultCallback)}
 *
 * @author frank.chen021@outlook.com
 * @date 12/8/25 10:32 pm
 */
public class DefaultServerConnection$ExecuteProtocolAsync extends BeforeInterceptor {
    private final MongoDbMetricRegistry metricRegistry = MongoDbMetricRegistry.get();

    @Override
    public void before(AopContext aopContext) throws Exception {
        Object protocol = aopContext.getArgs()[0];
        if (!(protocol instanceof IBithonObject)) {
            LoggerAdaptor.getLogger(DefaultServerConnection$ExecuteProtocolAsync.class)
                         .warn("Unknown Command", new RuntimeException());
            return;
        }

        //TODO: wrap callback and exception callback
        /*
         * IBithonObject bithonObject = (IBithonObject) protocol;
         * MongoCommand command = (MongoCommand) bithonObject.getInjectedObject();
         * SingleResultCallback callback = (SingleResultCallback) aopContext.getArgs()[1];
         */
    }
}
