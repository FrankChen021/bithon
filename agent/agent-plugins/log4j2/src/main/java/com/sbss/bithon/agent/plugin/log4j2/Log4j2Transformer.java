package com.sbss.bithon.agent.plugin.log4j2;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : <br>
 * Date: 18/4/13
 *
 * @author 马至远
 */
public class Log4j2Transformer extends AbstractClassTransformer {
    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(Log4j2Handler.class,
                                                    new MethodPointCut("org.apache.logging.log4j.core.Logger",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("logMessage",
                                                                                                              "java.lang.String",
                                                                                                              "org.apache.logging.log4j.Level",
                                                                                                              "org.apache.logging.log4j.Marker",
                                                                                                              "org.apache.logging.log4j.message.Message",
                                                                                                              "java.lang.Throwable")))};
    }
}
