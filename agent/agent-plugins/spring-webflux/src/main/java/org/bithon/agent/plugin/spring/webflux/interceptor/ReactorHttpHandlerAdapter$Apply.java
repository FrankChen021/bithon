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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsCollector;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * {@link org.springframework.http.server.reactive.ReactorHttpHandlerAdapter#apply}
 *
 * @author Frank Chen
 * @date 7/10/21 3:16 pm
 */
public class ReactorHttpHandlerAdapter$Apply extends AbstractInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(ReactorHttpHandlerAdapter$Apply.class);

    private HttpIncomingMetricsCollector metricCollector;
    private HttpIncomingFilter requestFilter;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("webflux-request-metrics",
                                                               HttpIncomingMetricsCollector.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        final HttpServerRequest request = (HttpServerRequest) aopContext.getArgs()[0];

        boolean shouldExclude = requestFilter.shouldBeExcluded(request.uri(),
                                                               request.requestHeaders().get("User-Agent"));
        return shouldExclude ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        final HttpServerRequest request = (HttpServerRequest) aopContext.getArgs()[0];
        final HttpServerResponse response = (HttpServerResponse) aopContext.getArgs()[1];

        Mono<Void> mono = aopContext.castReturningAs();
        if (aopContext.hasException() || mono.equals(Mono.empty())) {
            update(request, response, aopContext.getCostTime());
            return;
        }

        final long start = System.nanoTime();
        aopContext.setReturning(mono.doOnSuccessOrError((success, error) -> {
            try {
                update(request, response, System.nanoTime() - start);
            } catch (Exception e) {
                LOG.error("failed to record http incoming metrics", e);
            }
        }));
    }

    private void update(HttpServerRequest request, HttpServerResponse response, long responseTime) {
        String uri = request.fullPath();

        String srcApplication = request.requestHeaders().get(ITracePropagator.BITHON_SRC_APPLICATION);

        int httpStatus = response.status().code();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;

        this.metricCollector.getOrCreateMetrics(srcApplication, uri, httpStatus)
                            .updateRequest(responseTime, count4xx, count5xx);
    }
}
