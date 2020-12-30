package com.sbss.bithon.agent.plugin.undertow;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

public class UndertowTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(WorkerHandler.class,
                                                    new MethodPointCut("io.undertow.Undertow",
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs("start"))),
            new AgentHandler(OpenListenerHandler.class,
                             new MethodPointCut("io.undertow.server.protocol.http.HttpOpenListener",
                                                DefaultMethodNameMatcher.byNameAndArgs("setRootHandler",
                                                                                       "io.undertow.server.HttpHandler"))),
            new AgentHandler(ServerExchangeHandler.class,
                             new MethodPointCut("io.undertow.server.HttpServerExchange",
                                                DefaultMethodNameMatcher.byNameAndArgs("dispatch",
                                                                                       "java.util.concurrent.Executor",
                                                                                       "io.undertow.server.HttpHandler"))),

            new AgentHandler(UndertowErrorHandler.class,
                             new MethodPointCut("io.undertow.servlet.api.LoggingExceptionHandler",
                                                DefaultMethodNameMatcher.byNameAndArgs("handleThrowable",
                                                                                       new String[]{"io.undertow.server.HttpServerExchange",
                                                                                           "javax.servlet.ServletRequest",
                                                                                           "javax.servlet.ServletResponse",
                                                                                           "java.lang.Throwable"})))};
    }
}
