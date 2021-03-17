package com.sbss.bithon.agent.plugin.mongodb38;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.precondition.IPluginInstallationChecker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class MongoDbPlugin extends AbstractPlugin {

    @Override
    public List<IPluginInstallationChecker> getCheckers() {
        return Collections.singletonList(
            IPluginInstallationChecker.hasClass("com.mongodb.internal.connection.DefaultServerConnection")
        );
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            //request statistics

            forClass("com.mongodb.internal.connection.DefaultServerConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeProtocol")
                                                   .to(MongoDbPlugin.class.getPackage()
                                                       + ".ServerConnectionInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("executeProtocolAsync")
                                                   .to(MongoDbPlugin.class.getPackage()
                                                       + ".ServerConnectionInterceptor")
                ),


            //request bytes statistics
            forClass("com.mongodb.internal.connection.InternalStreamConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendMessage")
                                                   .to(MongoDbPlugin.class.getPackage()
                                                       + ".MongoDbByteHandlerInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("sendMessageAsync")
                                                   .to(MongoDbPlugin.class.getPackage()
                                                       + ".MongoDbByteHandlerInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("receiveMessage")
                                                   .to(MongoDbPlugin.class.getPackage()
                                                       + ".MongoDbByteHandlerInterceptor")

                    //TODO: how to count on asynchronous operations ???
                    //receiveMessageAsync
                )
        );
    }
}
