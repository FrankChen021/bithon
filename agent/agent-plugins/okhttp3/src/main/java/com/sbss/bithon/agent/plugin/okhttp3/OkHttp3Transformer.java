package com.sbss.bithon.agent.plugin.okhttp3;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : OkHttp3Transformer, for version > 3.3 <br>
 * Date: 17/10/23
 *
 * @author 马至远
 */
public class OkHttp3Transformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(OkHttp3Handler.class,
                                                    new MethodPointCut("okhttp3.RealCall",
                                                                       DefaultMethodNameMatcher.byNameAndEmptyArgs("getResponseWithInterceptorChain")))};
    }
}
