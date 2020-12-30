package com.sbss.bithon.agent.plugin.httpcomponent;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : apache http component transformer <br>
 * Date: 17/10/27
 *
 * @author 马至远
 */
public class HttpComponentTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler("com.sbss.bithon.agent.plugin.httpcomponent.HttpComponentHandler",
                                                    new MethodPointCut("org.apache.http.impl.client.InternalHttpClient",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("doExecute",
                                                                                                              "org.apache.http.HttpHost",
                                                                                                              "org.apache.http.HttpRequest",
                                                                                                              "org.apache.http.protocol.HttpContext")),
                                                    new MethodPointCut("org.apache.http.impl.execchain.RedirectExec",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                                                              "org.apache.http.conn.routing.HttpRoute",
                                                                                                              "org.apache.http.client.methods.HttpRequestWrapper",
                                                                                                              "org.apache.http.client.protocol.HttpClientContext",
                                                                                                              "org.apache.http.client.methods.HttpExecutionAware")),
                                                    new MethodPointCut("org.apache.http.impl.execchain.MinimalClientExec",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                                                              "org.apache.http.conn.routing.HttpRoute",
                                                                                                              "org.apache.http.client.methods.HttpRequestWrapper",
                                                                                                              "org.apache.http.client.protocol.HttpClientContext",
                                                                                                              "org.apache.http.client.methods.HttpExecutionAware")),
                                                    new MethodPointCut("org.apache.http.impl.client.DefaultRequestDirector",
                                                                       DefaultMethodNameMatcher.byNameAndArgs("execute",
                                                                                                              "org.apache.http.HttpHost",
                                                                                                              "org.apache.http.HttpRequest",
                                                                                                              "org.apache.http.protocol.HttpContext")))};
    }
}
