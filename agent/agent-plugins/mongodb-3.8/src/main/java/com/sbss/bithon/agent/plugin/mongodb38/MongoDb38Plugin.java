package com.sbss.bithon.agent.plugin.mongodb38;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.precondition.IPluginInstallationChecker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * @author frankchen
 */
public class MongoDb38Plugin extends AbstractPlugin {

    @Override
    public List<IPluginInstallationChecker> getCheckers() {
        return Collections.singletonList(
            IPluginInstallationChecker.hasClass("com.mongodb.internal.connection.DefaultServerConnection")
        );
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.mongodb.internal.connection.CommandHelper")
                .methods(MethodPointCutDescriptorBuilder.build()
                                                        .onAllMethods("executeCommand")
                                                        .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.CommandHelper$ExecuteCommand")
                ),

            forClass("com.mongodb.internal.connection.CommandProtocolImpl")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(takesArguments(9))
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$Constructor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("execute")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$Execute"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeAsync")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.CommandProtocolImpl$ExecuteAsync")
                ),

            //request statistics
            forClass("com.mongodb.internal.connection.DefaultServerConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeProtocol")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.DefaultServerConnectionExecuteProtocol"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeProtocolAsync")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.DefaultServerConnectionExecuteProtocolAsync")
                ),

            //request bytes statistics
            // By contrast to 3.4, ConnectionMessageSentEvent & ConnectionMessageReceivedEvent are removed
            // So we have to intercept the underlying StreamConnection to get the message size
            forClass("com.mongodb.internal.connection.InternalStreamConnection")
                .debug()
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendMessage")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnectionSendMessage"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendMessageAsync")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnectionSendMessageAsync"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("receiveMessageWithAdditionalTimeout")
                                                   .to("com.sbss.bithon.agent.plugin.mongodb38.interceptor.InternalStreamConnectionReceiveMessage")
                )
        );
    }
}
