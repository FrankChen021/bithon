package com.sbss.bithon.agent.plugin.mongodb;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class MongoDbPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.mongodb.connection.DefaultServerConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("executeProtocol",
                                         "com.mongodb.connection.Protocol<T>")
                        .to("com.sbss.bithon.agent.plugin.mongodb.MongoDbHandler"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("executeProtocolAsync",
                                         "com.mongodb.connection.Protocol<T>",
                                         "com.mongodb.async.SingleResultCallback<T>")
                        .to("com.sbss.bithon.agent.plugin.mongodb.MongoDbHandler")
                ),

            forClass("com.mongodb.event.ConnectionMessagesSentEvent")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onConstructor("com.mongodb.connection.ConnectionId",
                                       "int", "int")
                        .to("com.sbss.bithon.agent.plugin.mongodb.MongoDbHandler")
                ),

            forClass("com.mongodb.event.ConnectionMessageReceivedEvent")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onConstructor("com.mongodb.connection.ConnectionId",
                                       "int", "int")
                        .to("com.sbss.bithon.agent.plugin.mongodb.MongoDbHandler")
                )
        );
    }

//                new InterceptorDescriptor() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.sbss.commons.agent.plugin.mongodb.MongoDbTraceHandler";
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{
//                                new AgentPointcut() {
//                                    @Override
//                                    public ITargetClassMatcher getClassMatcher() {
//                                        return TargetClassNameMatcher.byName("com.mongodb.connection.DefaultServerConnection");
//                                    }
//
//                                    @Override
//                                    public TargetMethodMatcher getMethodMatcher() {
//                                        return TargetMethodMatcher.byNameAndArgs("executeProtocol", new String[]{"com.mongodb.connection.Protocol<T>"});
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public ITargetClassMatcher getClassMatcher() {
//                                        return TargetClassNameMatcher.byName("com.mongodb.connection.DefaultServerConnection");
//                                    }
//
//                                    @Override
//                                    public TargetMethodMatcher getMethodMatcher() {
//                                        return TargetMethodMatcher.byNameAndArgs("executeProtocolAsync", new String[]{"com.mongodb.connection.Protocol<T>", "com.mongodb.async.SingleResultCallback<T>"});
//                                    }
//                                },
//                        };
//                    }
//                }
//        };

}
