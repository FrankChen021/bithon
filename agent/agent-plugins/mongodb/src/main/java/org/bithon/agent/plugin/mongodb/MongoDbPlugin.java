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

package org.bithon.agent.plugin.mongodb;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class MongoDbPlugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return IInterceptorPrecondition.isClassDefined("com.mongodb.connection.DefaultServerConnection");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            /*
             * See CommandHelper$ExecuteCommand
             * Since this class is internal, and we need to call its method,
             * one way is to turn it into IBithonObject and cache the value we need in the injected object
             */
            forClass("com.mongodb.connection.InternalStreamConnection")
                .debug()
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.InternalStreamConnection$Constructor")
                .build(),

            forClass("com.mongodb.connection.DefaultServerConnection")
                .onMethodAndArgs("executeProtocol", "com.mongodb.connection.Protocol<T>")
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.DefaultServerConnection$ExecuteProtocol")

                .onMethodAndArgs("executeProtocolAsync",
                                 "com.mongodb.connection.Protocol<T>", "com.mongodb.async.SingleResultCallback<T>")
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.DefaultServerConnection$ExecuteProtocolAsync")
                .build(),

            forClass("com.mongodb.connection.CommandHelper")
                .debug()
                .onMethodName("executeCommand")
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.CommandHelper$ExecuteCommand")

                .onMethodName("executeCommandAsync")
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.CommandHelper$ExecuteCommandAsync")
                .build(),

            forClass("com.mongodb.event.ConnectionMessagesSentEvent")
                .debug()
                .onConstructor("com.mongodb.connection.ConnectionId",
                               "int", "int")
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.ConnectionMessagesSentEvent$Constructor")
                .build(),

            forClass("com.mongodb.event.ConnectionMessageReceivedEvent")
                .debug()
                .onConstructor("com.mongodb.connection.ConnectionId",
                               "int", "int")
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.ConnectionMessageReceivedEvent$Constructor")
                .build(),

            forClass("com.mongodb.connection.CommandProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$CommandProtocol")
                .build(),

            forClass("com.mongodb.connection.DeleteCommandProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$DeleteCommandProtocol")
                .build(),

            forClass("com.mongodb.connection.DeleteProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$DeleteProtocol")
                .build(),

            forClass("com.mongodb.connection.GetMoreProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$GetMoreProtocol")
                .build(),

            forClass("com.mongodb.connection.InsertCommandProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$InsertCommandProtocol")
                .build(),

            forClass("com.mongodb.connection.InsertProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$InsertProtocol")
                .build(),

            forClass("com.mongodb.connection.KillCursorProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$KillCursorProtocol")
                .build(),

            forClass("com.mongodb.connection.QueryProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$QueryProtocol")
                .build(),

            forClass("com.mongodb.connection.UpdateCommandProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$UpdateCommandProtocol")
                .build(),

            forClass("com.mongodb.connection.UpdateProtocol")
                .onAllConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb.interceptor.Protocol$UpdateProtocol")
                .build()
        );
    }
}
