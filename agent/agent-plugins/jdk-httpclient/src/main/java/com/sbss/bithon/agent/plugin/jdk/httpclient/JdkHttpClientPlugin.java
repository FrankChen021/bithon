package com.sbss.bithon.agent.plugin.jdk.httpclient;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MatcherUtils;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forBootstrapClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isStatic;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * jdk http-connection plugin
 *
 * @author frankchen
 */
public class JdkHttpClientPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forBootstrapClass("sun.net.www.http.HttpClient")
                .debug()
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethod(named("New").and(isStatic()).and(MatcherUtils.takesArgument(4, "sun.net.www.protocol.http.HttpURLConnection")))
                        .to("com.sbss.bithon.agent.plugin.jdk.httpclient.HttpClientNewInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("writeRequests", "sun.net.www.MessageHeader")
                        .to("com.sbss.bithon.agent.plugin.jdk.httpclient.HttpClientWriteRequestInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("writeRequests", "sun.net.www.MessageHeader", "sun.net.www.http.PosterOutputStream")
                        .to("com.sbss.bithon.agent.plugin.jdk.httpclient.HttpClientWriteRequestInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethod(named("parseHTTP").and(MatcherUtils.takesArgument(0, "sun.net.www.MessageHeader")))
                        .to("com.sbss.bithon.agent.plugin.jdk.httpclient.HttpClientParseHttpInterceptor"))

        );
    }
}
