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

package org.bithon.agent.plugin.mongodb38.interceptor.protocol;

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.mongo.MongoCommand;

import java.util.List;

/**
 * {@link com.mongodb.connection.DeleteCommandProtocol#DeleteCommandProtocol(MongoNamespace, boolean, WriteConcern, List)}
 */
public class DeleteCommandProtocol$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        MongoNamespace ns = aopContext.getArgAs(0);
        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(new MongoCommand(ns.getDatabaseName(),
                                                        ns.getCollectionName(),
                                                        "DeleteCommand"));
    }
}
