package com.sbss.bithon.agent.plugin.okhttp32;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : OkHttp3Transformer, for version < 3.2 <br>
 * Date: 17/10/23
 *
 * @author 马至远
 */
public class OkHttp32Transformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
//        @Override
//        public Map<String, String> getProperties() {
//            Map<String, String> properties = new HashMap<>();
//            properties.put("ignoredSuffixes", );
//            return properties;
//        }
        return new IAgentHandler[]{new AgentHandler(OkHttp32Handler.class,
                                                    new MethodPointCut("okhttp3.RealCall",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("getResponseWithInterceptorChain",
                                                                                                              "boolean"))),};
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.okhttp32.OkHttp32TraceInterceptorHandler";
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
//                                        return DefaultClassNameMatcher.byName("okhttp3.internal.http.BridgeInterceptor");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byName("intercept");
//                                    }
//                                }
//                        };
//                    }
//                },
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.okhttp32.OkHttp32TraceRequestHandler";
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
//                                        return DefaultClassNameMatcher.byName("okhttp3.Request$Builder");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byName("build");
//                                    }
//                                }
//                        };
//                    }
//                }
//        };
    }
}
