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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;
import org.bithon.agent.plugin.spring.webflux.metric.HttpBodySizeCollector;
import org.bithon.agent.plugin.spring.webflux.metric.HttpIOMetrics;
import reactor.netty.NettyPipeline;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.HttpInfos;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * get body size of HttpRequest and HttpResponse
 * <p>
 * {@link reactor.netty.http.server.HttpServerConfig.HttpServerChannelInitializer#onChannelInit}
 *
 * @author frank.chen021@outlook.com
 * @date 7/10/21 4:15 pm
 */
public class HttpServerChannelInitializer$OnChannelInit extends AbstractInterceptor {

    private final HttpIncomingMetricsRegistry metricsRegistry = HttpIncomingMetricsRegistry.get();

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
               .addAfter(NettyPipeline.HttpTrafficHandler,
                         NettyPipeline.HttpMetricsHandler,
                         new HttpServerBodySizeCollector(metricsRegistry));
    }

    private static class HttpServerBodySizeCollector extends HttpBodySizeCollector {
        private static Class<?> httpServerOperationClass;

        static {
            // HttpServerOperations's visibility is defined as package-level
            try {
                httpServerOperationClass = Class.forName("reactor.netty.http.server.HttpServerOperations",
                                                         false,
                                                         ChannelHandlerContext.class.getClassLoader());
            } catch (ClassNotFoundException ignored) {
                LOG.error("Unable to find HttpServerOperations. HTTP metrics may not work as expected.");
            }
        }

        private final HttpIncomingMetricsRegistry metricCollector;

        private HttpServerBodySizeCollector(HttpIncomingMetricsRegistry metricCollector) {
            this.metricCollector = metricCollector;
        }

        @Override
        protected void updateBytes(ChannelOperations<?, ?> channelOps,
                                   long dataReceived,
                                   long dataSent,
                                   long receivedTimeNs) {
            try {
                metricCollector.getOrCreateMetrics(((HttpServerRequest) channelOps).requestHeaders()
                                                                                   .get(ITracePropagator.TRACE_HEADER_SRC_APPLICATION),
                                                   ((HttpInfos) channelOps).method().name(),
                                                   ((HttpInfos) channelOps).fullPath(),
                                                   ((HttpServerResponse) channelOps).status().code())
                               .updateBytes(dataReceived, dataSent);
            } catch (Exception e) {
                LOG.error("exception when update io metrics", e);
            }
        }

        /**
         * @param bithonObject Type of ChannelOperations<?, ?>
         *                     In this case, it's type of {@link reactor.netty.http.server.HttpServerOperations}.
         *                     And the injected object is assigned in {@link HttpServerOperations$Ctor}
         */
        @Override
        protected HttpIOMetrics getMetricContext(IBithonObject bithonObject) {
            // raw type is HttpServerOperations
            return ((HttpServerContext) (bithonObject).getInjectedObject()).getMetrics();
        }

        @Override
        protected Class<?> getTargetClass() {
            return httpServerOperationClass;
        }
    }
}
