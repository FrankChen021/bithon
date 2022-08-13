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

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.aop.precondition.IInterceptorPrecondition;
import org.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class MongoDb38Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return IInterceptorPrecondition.hasClass("com.mongodb.internal.connection.DefaultServerConnection");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.mongodb.internal.connection.CommandHelper")
                .methods(MethodPointCutDescriptorBuilder.build()
                                                        .onAllMethods("executeCommand")
                                                        .to("org.bithon.agent.plugin.mongodb38.interceptor.CommandHelper$ExecuteCommand")
                ),

            forClass("com.mongodb.internal.connection.CommandProtocolImpl")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.argumentSize(size -> size >= 9))
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$Constructor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("execute")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$Execute"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeAsync")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$ExecuteAsync")
                ),

            //request statistics
            forClass("com.mongodb.internal.connection.DefaultServerConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeProtocol")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.DefaultServerConnection$ExecuteProtocol"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeProtocolAsync")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.DefaultServerConnection$ExecuteProtocolAsync")
                ),

            //request bytes statistics
            // By contrast to 3.4, ConnectionMessageSentEvent & ConnectionMessageReceivedEvent are removed
            // So we have to intercept the underlying StreamConnection to get the message size
            forClass("com.mongodb.internal.connection.InternalStreamConnection")
                .debug()
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendMessage")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$SendMessage"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendMessageAsync")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$SendMessageAsync"),

                    // 3.8
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("receiveMessage")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$ReceiveMessage"),

                    // 4.x, following method replaces underlying 'receiveMessage'
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("receiveMessageWithAdditionalTimeout")
                                                   .to("org.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnection$ReceiveMessage")
                )
        );
    }
}
