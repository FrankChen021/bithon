package com.sbss.bithon.agent.plugin.httpclient.jdk;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MatcherUtils;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forBootstrapClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.*;

/**
 * jdk http-connection plugin
 *
 * @author frankchen
 */
public class JdkHttpClientPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
                forBootstrapClass("sun.net.www.http.HttpClient")
                        .methods(
                                MethodPointCutDescriptorBuilder.build()
                                        .onMethod(named("New")
                                                .and(isStatic())
                                                .and(takesArguments(5))
                                                .and(MatcherUtils.takesArgument(4, "sun.net.www.protocol.http.HttpURLConnection")))
                                        .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientNewInterceptor"),

                                MethodPointCutDescriptorBuilder.build()
                                        .onMethodAndArgs("writeRequests", "sun.net.www.MessageHeader")
                                        .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientWriteRequestInterceptor"),

                                MethodPointCutDescriptorBuilder.build()
                                        .onMethodAndArgs("writeRequests", "sun.net.www.MessageHeader", "sun.net.www.http.PosterOutputStream")
                                        .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientWriteRequestInterceptor"),

                                MethodPointCutDescriptorBuilder.build()
                                        .onMethod(named("parseHTTP").and(MatcherUtils.takesArgument(0, "sun.net.www.MessageHeader")))
                                        .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientParseHttpInterceptor")),

                // HttpsClient inherits from HttpClient
                forBootstrapClass("sun.net.www.protocol.https.HttpsClient")
                        .methods(
                                MethodPointCutDescriptorBuilder.build()
                                        .onMethod(named("New")
                                                .and(isStatic())
                                                .and(takesArguments(7))
                                                // there're two overridden versions of 'New' both of which have 7 parameters
                                                // and they don't share the 3rd parameter
                                                .and(MatcherUtils.takesArgument(3, "java.net.Proxy"))
                                                .and(MatcherUtils.takesArgument(6, "sun.net.www.protocol.http.HttpURLConnection")))
                                        .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpsClientNewInterceptor"))

        );
    }
}
