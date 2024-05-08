/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.plugin.httpclient.jdk;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * jdk http-connection plugin
 * <p>
 * Following simple diagram demonstrates flow of all intercepted methods in this plugin
 * <p>
 * URLConnection ---> HttpClient.New ---> HttpClient.ctor(not intercepted)
 * ---> NetworkClient.openServer
 * ---> NetworkClient.doConnect (Socket returned)
 * ---> HttpClient.writeRequests
 * ---> Socket.getOutputStream
 * ---> HttpClient.parseHttp
 * ---> Socket.getInputStream
 *
 * @author frankchen
 */
public class JdkHttpClientPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("java.net.Socket")
                .onMethod("getInputStream")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.Socket$GetInputStream")

                .onMethod("getOutputStream")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.Socket$GetOutputStream")
                .build(),

            // for HTTPS
            forClass("sun.security.ssl.SSLSocketImpl")
                .onMethod("getInputStream")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.Socket$GetInputStream")

                .onMethod("getOutputStream")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.Socket$GetOutputStream")
                .build(),

            forClass("sun.net.NetworkClient")
                .onMethod("doConnect")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.NetworkClient$DoConnect")
                .build(),

            forClass("sun.net.www.http.HttpClient")
                .onMethod(ElementMatchers.named("New")
                                         .and(ElementMatchers.isStatic()))
                .andArgsSize(5)
                .andArgs(4, "sun.net.www.protocol.http.HttpURLConnection")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$New")

                .onMethod("writeRequests")
                .andArgs("sun.net.www.MessageHeader")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$WriteRequests")

                .onMethod("writeRequests")
                .andArgs("sun.net.www.MessageHeader", "sun.net.www.http.PosterOutputStream")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$WriteRequests")

                .onMethod("parseHTTP")
                .andArgs(0, "sun.net.www.MessageHeader")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpClient$ParseHTTP")
                .build(),

            forClass("sun.net.www.protocol.https.HttpsURLConnectionImpl")
                .onMethod("connect")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpURLConnection$Connect")
                .build(),

            forClass("sun.net.www.protocol.http.HttpURLConnection")
                .onMethod("connect")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpURLConnection$Connect")
                .build(),

            // HttpsClient inherits from HttpClient
            forClass("sun.net.www.protocol.https.HttpsClient")
                .onMethod(ElementMatchers.named("New")
                                         .and(ElementMatchers.isStatic()))
                .andArgsSize(7)
                // there are two overridden versions of 'New' both of which have 7 parameters,
                // and they don't share the 3rd parameter
                .andArgs(3, "java.net.Proxy")
                .andArgs(6, "sun.net.www.protocol.http.HttpURLConnection")
                .interceptedBy("org.bithon.agent.plugin.httpclient.jdk.interceptor.HttpsClient$New")
                .build()
        );
    }
}
