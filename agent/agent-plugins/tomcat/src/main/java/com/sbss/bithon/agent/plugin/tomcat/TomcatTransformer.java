package com.sbss.bithon.agent.plugin.tomcat;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

public class TomcatTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(EndpointHandler.class,
                                                    new MethodPointCut("org.apache.tomcat.util.net.AbstractEndpoint",
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs("start"))),
            new AgentHandler(RequestHandler.class,
                             new MethodPointCut("org.apache.catalina.connector.CoyoteAdapter",
                                                DefaultMethodNameMatcher.byNameAndArgs("service",
                                                                                       "org.apache.coyote.Request",
                                                                                       "org.apache.coyote.Response"))),

            new AgentHandler(TomcatExceptionHandler.class,
                             new MethodPointCut("org.apache.catalina.core.StandardWrapperValve",
                                                DefaultMethodNameMatcher.byNameAndArgs("exception",
                                                                                       "org.apache.catalina.connector.Request",
                                                                                       "org.apache.catalina.connector.Response",
                                                                                       "java.lang.Throwable"))),};
//                new AgentHandler() {
//                    @Override
//                    public String getHandlerClass() {
//                        return "com.keruyun.commons.agent.plugin.tomcat.TomcatTraceHandler";
//                    }
//
//                    @Override
//                    public Map<String, String> getProperties() {
//                        Map<String, String> properties = new HashMap<>();
//                        properties.put("ignoredSuffixes", "html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg");
//                        return properties;
//                    }
//
//                    @Override
//                    public AgentPointcut[] getPointcuts() {
//                        return new AgentPointcut[]{
//                                new AgentPointcut() {
//                                    @Override
//                                    public AgentClassMatcher getClassMatcher() {
//                                        return DefaultClassNameMatcher.byName("org.apache.catalina.core.StandardHostValve");
//                                    }
//
//                                    @Override
//                                    public AgentMethodMatcher getMethodMatcher() {
//                                        return DefaultMethodNameMatcher.byNameAndArgs("invoke", new String[]{"org.apache.catalina.connector.Request", "org.apache.catalina.connector.Response"});
//                                    }
//                                }
//                        };
//                    }
//                }
    }
}
