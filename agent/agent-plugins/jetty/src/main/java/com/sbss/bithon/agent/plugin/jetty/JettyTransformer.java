package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

public class JettyTransformer extends AbstractClassTransformer {
    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(ConnectorHandler.class,
                                                    MethodPointCut.New("org.eclipse.jetty.server.AbstractConnector",
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs("doStart"))),

            new AgentHandler(ThreadPoolHandler.class,
                             MethodPointCut.New("org.eclipse.jetty.util.thread.QueuedThreadPool",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("doStart"))),

            new AgentHandler(RequestHandler.class,
                             MethodPointCut.New("org.eclipse.jetty.server.handler.ContextHandler",
                                                DefaultMethodNameMatcher.byNameAndArgs("doHandle",
                                                                                       "java.lang.String",
                                                                                       "org.eclipse.jetty.server.Request",
                                                                                       "javax.servlet.http.HttpServletRequest",
                                                                                       "javax.servlet.http.HttpServletResponse")))

        };
    }
}
