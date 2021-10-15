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
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.plugin.spring.webflux.metric.HttpBodySizeCollector;
import org.bithon.agent.plugin.spring.webflux.metric.HttpIncomingRequestMetricCollector;
import reactor.netty.NettyPipeline;

/**
 * get body size of HttpRequest and HttpResponse
 * <p>
 * {@link reactor.netty.http.server.HttpServerConfig.HttpServerChannelInitializer#onChannelInit}
 *
 * @author Frank Chen
 * @date 7/10/21 4:15 pm
 */
public class HttpServerChannelInitializer$OnChannelInit extends AbstractInterceptor {

    private HttpIncomingRequestMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("webflux-request-metrics",
                                                               HttpIncomingRequestMetricCollector.class);

        return true;
    }

    /**
     * Hook a handler to the channel
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
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
                         new HttpBodySizeCollector(metricCollector));
    }
}
