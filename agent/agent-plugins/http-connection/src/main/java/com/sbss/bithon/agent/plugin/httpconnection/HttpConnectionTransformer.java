package com.sbss.bithon.agent.plugin.httpconnection;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.AgentClassMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.AgentMethodMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultClassNameMatcher;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IMethodPointCut;

/**
 * Description : jdk http-connection transformer <br>
 * Date: 17/10/25
 *
 * @author 马至远
 */
public class HttpConnectionTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new IAgentHandler() {
            @Override
            public String getHandlerClass() {
                return "com.sbss.bithon.agent.plugin.httpconnection.HttpConnectionHandler";
            }

            @Override
            public IMethodPointCut[] getPointcuts() {
                return new IMethodPointCut[]{new IMethodPointCut() {
                    @Override
                    public AgentClassMatcher getClassMatcher() {
                        return DefaultClassNameMatcher.byName("sun.net.www.protocol.http.HttpURLConnection");
                    }

                    @Override
                    public AgentMethodMatcher getMethodMatcher() {
                        return DefaultMethodNameMatcher.byNameAndArgs("getRequestMethod", null);
                    }
                }};
            }
        }};
    }
}
