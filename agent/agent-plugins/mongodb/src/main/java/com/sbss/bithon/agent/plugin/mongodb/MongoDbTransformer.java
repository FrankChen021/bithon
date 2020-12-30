package com.sbss.bithon.agent.plugin.mongodb;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : mongodb transformer <br>
 * Date: 17/11/3
 *
 * @author 马至远
 */
public class MongoDbTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(MongoDbHandler.class,
                                                    new MethodPointCut("com.mongodb.connection.DefaultServerConnection",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("executeProtocol",
                                                                                                              "com.mongodb.connection.Protocol<T>")),
                                                    new MethodPointCut("com.mongodb.connection.DefaultServerConnection",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("executeProtocolAsync",
                                                                                                              "com.mongodb.connection.Protocol<T>",
                                                                                                              "com.mongodb.async.SingleResultCallback<T>")),
                                                    new MethodPointCut("com.mongodb.event.ConnectionMessagesSentEvent",
                                                                       DefaultMethodNameMatcher.byConstructorAndArgs(new String[]{"com.mongodb.connection.ConnectionId",
                                                                           "int",
                                                                           "int"})),
                                                    new MethodPointCut("com.mongodb.event.ConnectionMessageReceivedEvent",
                                                                       DefaultMethodNameMatcher.byConstructorAndArgs(new String[]{"com.mongodb.connection.ConnectionId",
                                                                           "int",
                                                                           "int"})))};
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.mongodb.MongoDbTraceHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        return null;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName("com.mongodb.connection.DefaultServerConnection");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs("executeProtocol", new String[]{"com.mongodb.connection.Protocol<T>"});
//                                    }
//                                },
//
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName("com.mongodb.connection.DefaultServerConnection");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs("executeProtocolAsync", new String[]{"com.mongodb.connection.Protocol<T>", "com.mongodb.async.SingleResultCallback<T>"});
//                                    }
//                                },
//                        };
//                    }
//                }
//        };
    }
}
