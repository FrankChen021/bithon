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

package org.bithon.agent.plugin.httpclient.reactor.interceptor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.http.HttpIOMetrics;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.plugin.httpclient.reactor.metric.HttpBodySizeCollector;
import reactor.netty.NettyPipeline;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.client.HttpClientInfos;
import reactor.netty.http.client.HttpClientRequest;

/**
 * get body size of HttpRequest and HttpResponse
 * <p>
 * {@link reactor.netty.http.client.HttpClientConfig.HttpClientChannelInitializer#OnChannelInit}
 *
 * @author frank.chen021@outlook.com
 * @date 7/10/21 4:15 pm
 */
public class HttpClientChannelInitializer$OnChannelInit extends AfterInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    /**
     * Hook a handler to the channel
     */
    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        //
        // in netty, it's a bit complicated to get the size of a request
        // maybe in future we can do it by intercepting the HttpRequestDecoder
        //
        Channel channel = aopContext.getArgAs(1);
        channel.pipeline()
               .addBefore(NettyPipeline.ReactiveBridge,
                          NettyPipeline.HttpMetricsHandler,
                          new HttpClientBodySizeCollector(metricRegistry));
    }

    private static class HttpClientBodySizeCollector extends HttpBodySizeCollector {
        private static Class<?> httpClientOperationClass;

        static {
            // HttpClientOperations's visibility is defined as package-level
            try {
                httpClientOperationClass = Class.forName("reactor.netty.http.client.HttpClientOperations",
                                                         false,
                                                         ChannelHandlerContext.class.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                LOG.error("Unable to find HttpClientOperations. HTTP metrics may not work as expected.");
            }
        }

        private final HttpOutgoingMetricsRegistry metricRegistry;

        private HttpClientBodySizeCollector(HttpOutgoingMetricsRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
        }

        @Override
        protected void updateBytes(ChannelOperations<?, ?> channelOps,
                                   long dataReceived,
                                   long dataSent,
                                   long receivedTimeNs) {
            // resource url is the fully qualified URL
            metricRegistry.addBytes(((HttpClientInfos) channelOps).resourceUrl(),
                                    ((HttpClientRequest) channelOps).method().name(),
                                    dataSent,
                                    dataReceived);
        }

        @Override
        protected HttpIOMetrics getMetricContext(IBithonObject bithonObject) {
            // raw type is HttpClientOperations
            return (HttpIOMetrics) bithonObject.getInjectedObject();
        }

        @Override
        protected Class<?> getTargetClass() {
            return httpClientOperationClass;
        }
    }
}
