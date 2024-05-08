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

package org.bithon.agent.plugin.mongodb38;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class MongoDb38Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return IInterceptorPrecondition.isClassDefined("com.mongodb.internal.connection.DefaultServerConnection");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.mongodb.internal.connection.CommandHelper")
                .onMethod("executeCommand")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.CommandHelper$ExecuteCommand")
                .build(),

            forClass("com.mongodb.internal.connection.CommandProtocolImpl")
                .onConstructor(Matchers.argumentSize(size -> size >= 9))
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$Ctor")

                .onMethod("execute")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$Execute")

                .onMethod("executeAsync")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$ExecuteAsync")
                .build(),

            //request statistics
            forClass("com.mongodb.internal.connection.DefaultServerConnection")
                .onMethod("executeProtocol")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.DefaultServerConnection$ExecuteProtocol")

                .onMethod("executeProtocolAsync")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.DefaultServerConnection$ExecuteProtocolAsync")
                .build(),

            //request bytes statistics
            // By contrast to 3.4, ConnectionMessageSentEvent & ConnectionMessageReceivedEvent are removed
            // So we have to intercept the underlying StreamConnection to get the message size
            forClass("com.mongodb.internal.connection.InternalStreamConnection")
                .onMethod("sendMessage")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$SendMessage")

                .onMethod("sendMessageAsync")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$SendMessageAsync")

                // 3.8
                .onMethod("receiveMessage")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$ReceiveMessage")

                // 4.x, following method replaces underlying 'receiveMessage'
                .onMethod("receiveMessageWithAdditionalTimeout")
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$ReceiveMessage")
                .build(),

            // Protocols
            forClass("com.mongodb.connection.CommandProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.CommandProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.DeleteCommandProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.DeleteCommandProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.DeleteProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.DeleteProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.GetMoreProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.GetMoreProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.InsertCommandProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.InsertCommandProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.InsertProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.InsertProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.KillCursorProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.KillCursorProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.QueryProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.QueryProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.UpdateCommandProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.UpdateCommandProtocol$Ctor")
                .build(),

            forClass("com.mongodb.connection.UpdateProtocol")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.mongodb38.interceptor.protocol.UpdateProtocol$Ctor")
                .build()
        );
    }
}
