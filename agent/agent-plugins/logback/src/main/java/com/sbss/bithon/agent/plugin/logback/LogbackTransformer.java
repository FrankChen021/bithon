package com.sbss.bithon.agent.plugin.logback;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : define logback pointcut and interceptor <br>
 * Date: 18/4/13
 *
 * @author 马至远
 */
public class LogbackTransformer extends AbstractClassTransformer {
    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(LogbackHandler.class,
                                                    new MethodPointCut("ch.qos.logback.classic.Logger",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("callAppenders",
                                                                                                              "ch.qos.logback.classic.spi.ILoggingEvent")))};
    }
}
