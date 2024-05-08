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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.spring.webflux.config.GatewayFilterConfigs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2021-09-22 23:36
 */
public class SpringWebFluxPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        List<InterceptorDescriptor> staticInterceptors = Arrays.asList(
            forClass("org.springframework.http.server.reactive.ReactorHttpHandlerAdapter")
                .hook()
                .onMethodName("apply")
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.ReactorHttpHandlerAdapter$Apply")
                .build(),

            forClass("reactor.netty.http.server.HttpServerConfig$HttpServerChannelInitializer")
                .hook()
                .onMethodName("onChannelInit")
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpServerChannelInitializer$OnChannelInit")
                .build(),

            forClass("reactor.netty.http.server.HttpServerOperations")
                .hook()
                // Its ctors vary in different versions, hook to all ctors
                .onAllConstructor()
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpServerOperations$Ctor")
                .build(),

            forClass("reactor.netty.http.client.HttpClientFinalizer")
                .hook()
                .onMethodName("send")
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpClientFinalizer$Send")

                .hook()
                .onMethodName("responseConnection")
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpClientFinalizer$ResponseConnection")
                .build(),

            forClass("reactor.netty.http.client.HttpClientConfig$HttpClientChannelInitializer")
                .hook()
                .onMethodName("onChannelInit")
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpClientChannelInitializer$OnChannelInit")
                .build(),

            forClass("reactor.netty.http.client.HttpClientOperations")
                .hook()
                .onAllConstructor()
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.HttpClientOperations$Ctor")
                .build(),

            forClass("reactor.core.publisher.Flux")
                .hook()
                .onMethodAndRawArgs(
                    "timeout",
                    "org.reactivestreams.Publisher",
                    "java.util.function.Function",
                    "org.reactivestreams.Publisher")
                .to("org.bithon.agent.plugin.spring.webflux.interceptor.Flux$Timeout")
                .build()
            );

        List<InterceptorDescriptor> interceptorDescriptors = getGatewayInterceptors();
        interceptorDescriptors.addAll(staticInterceptors);
        return interceptorDescriptors;
    }

    /**
     * since gateway filters have a similar pattern, we define them in the configuration file
     * so that user defined filters can be supported
     */
    List<InterceptorDescriptor> getGatewayInterceptors() {
        List<InterceptorDescriptor> filterInterceptors = new ArrayList<>();

        GatewayFilterConfigs configs = ConfigurationManager.getInstance().getConfig(GatewayFilterConfigs.class);
        for (Map.Entry<String, GatewayFilterConfigs.Filter> entry : configs.entrySet()) {
            String clazz = entry.getKey();
            GatewayFilterConfigs.Filter filter = entry.getValue();


            MethodPointCutDescriptorBuilder builder = forClass(clazz).debug()
                                                                     .hook()
                                                                     .onMethodAndArgs(
                                                                         "filter",
                                                                         "org.springframework.web.server.ServerWebExchange",
                                                                         "org.springframework.cloud.gateway.filter.GatewayFilterChain");


            String to;
            if ("before".equals(filter.getMode())) {
                to = "org.bithon.agent.plugin.spring.webflux.interceptor.BeforeGatewayFilter$Filter";
            } else if ("around".equals(filter.getMode())) {
                to = "org.bithon.agent.plugin.spring.webflux.interceptor.AroundGatewayFilter$Filter";
            } else {
                throw new RuntimeException("Invalid configuration");
            }

            filterInterceptors.add(builder.to(to).build());
        }

        return filterInterceptors;
    }
}
