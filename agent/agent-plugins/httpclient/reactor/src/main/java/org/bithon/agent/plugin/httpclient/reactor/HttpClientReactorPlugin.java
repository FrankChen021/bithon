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

package org.bithon.agent.plugin.httpclient.reactor;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2021-09-22 23:36
 */
public class HttpClientReactorPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // HttpClient is the super class of the following HttpClientFinalizer
            forClass("reactor.netty.http.client.HttpClient")
                .onMethod("request")
                .andArgs("io.netty.handler.codec.http.HttpMethod")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClient$Request")
                .build(),

            forClass("reactor.netty.http.client.HttpClientFinalizer")
                .onMethod("_connect")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientFinalizer$Connect")

                .onMethod("uri")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientFinalizer$Uri")

                .onMethod("send")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientFinalizer$Send")

                .onMethod("responseConnection")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientFinalizer$ResponseConnection")
                .build(),

            forClass("reactor.netty.http.client.HttpClientConfig")
                .onMethod("defaultConnectionObserver")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientConfig$DefaultConnectionObserver")
                .build(),

            forClass("reactor.netty.http.client.HttpClientConfig$HttpClientChannelInitializer")
                .onMethod("onChannelInit")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientChannelInitializer$OnChannelInit")
                .build(),

            forClass("reactor.netty.http.client.HttpClientOperations")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.HttpClientOperations$Ctor")
                .build(),

            forClass("reactor.core.publisher.Flux")
                .onMethod("timeout")
                .andRawArgs("org.reactivestreams.Publisher",
                            "java.util.function.Function",
                            "org.reactivestreams.Publisher")
                .interceptedBy("org.bithon.agent.plugin.httpclient.reactor.interceptor.Flux$Timeout")
                .build()
        );
    }
}
