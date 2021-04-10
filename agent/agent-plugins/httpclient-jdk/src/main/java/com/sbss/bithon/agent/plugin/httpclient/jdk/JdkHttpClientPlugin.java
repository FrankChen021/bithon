/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.plugin.httpclient.jdk;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MatcherUtils;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forBootstrapClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.isStatic;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.takesArguments;

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
                                                                 .and(MatcherUtils.takesArgument(4,
                                                                                                 "sun.net.www.protocol.http.HttpURLConnection")))
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientNewInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("writeRequests", "sun.net.www.MessageHeader")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientWriteRequestInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("writeRequests",
                                                                    "sun.net.www.MessageHeader",
                                                                    "sun.net.www.http.PosterOutputStream")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpClientWriteRequestInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(named("parseHTTP").and(MatcherUtils.takesArgument(0,
                                                                                                               "sun.net.www.MessageHeader")))
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
                                                                 .and(MatcherUtils.takesArgument(6,
                                                                                                 "sun.net.www.protocol.http.HttpURLConnection")))
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.jdk.HttpsClientNewInterceptor"))

        );
    }
}
