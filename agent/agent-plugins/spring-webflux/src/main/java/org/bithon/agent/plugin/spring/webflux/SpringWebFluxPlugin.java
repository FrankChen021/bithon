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

package org.bithon.agent.plugin.spring.webflux;

import org.bithon.agent.core.aop.descriptor.BithonClassDescriptor;
import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MatcherUtils;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2021-09-22 23:36
 */
public class SpringWebFluxPlugin implements IPlugin {

    @Override
    public BithonClassDescriptor getBithonClassDescriptor() {
        return BithonClassDescriptor.of(
            // used to store HttpServerContext
            "reactor.netty.http.server.HttpServerOperations"
        );
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.springframework.http.server.reactive.ReactorHttpHandlerAdapter")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("apply")
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.ReactorHttpHandlerAdapter$Apply")
                ),

            /*
            forClass("org.springframework.web.server.adapter.HttpWebHandlerAdapter")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("createExchange")
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpWebHandlerAdapter$CreateExchange")
                ),

            forClass("org.springframework.cloud.gateway.filter.NettyRoutingFilter")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(MatcherUtils.takesArguments(2)
                                                                         .and(MatcherUtils.named("getHttpClient")))
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.NettyRoutingFilter$GetHttpClient")
                ),*/

            forClass("reactor.netty.http.server.HttpServerConfig$HttpServerChannelInitializer")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("onChannelInit")
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpServerChannelInitializer$OnChannelInit")
                ),

            forClass("reactor.netty.http.server.HttpServerOperations")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(MatcherUtils.takesArguments(1)
                                                                              .or(MatcherUtils.takesArguments(10)))
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpServerOperations$Ctor")
                ),

            forClass("reactor.netty.http.client.HttpClientFinalizer")
                .debug()
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("send")
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpClientFinalizer$Send"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("responseConnection")
                                                   .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpClientFinalizer$ResponseConnection")
                )
        );
    }
}
