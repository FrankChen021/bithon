package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
import com.sbss.bithon.agent.plugin.undertow.metric.WebRequestMetricCollector;
import io.undertow.server.HttpServerExchange;

/**
 * @author frankchen
 */
public class HttpServerExchangeDispatch extends AbstractInterceptor {

    private UserAgentFilter userAgentFilter;
    private RequestUriFilter uriFilter;
    private WebRequestMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("undertow-web-request-metrics",
                                                               WebRequestMetricCollector.class);

        userAgentFilter = new UserAgentFilter();
        uriFilter = new RequestUriFilter();

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext context) {
        final HttpServerExchange exchange = context.castTargetAs();

        if (this.userAgentFilter.isFiltered(exchange.getRequestHeaders().getFirst("User-Agent"))
            || this.uriFilter.isFiltered(exchange.getRequestPath())) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        final long startTime = System.nanoTime();
        exchange.addExchangeCompleteListener((listener,
                                              next) -> {
            metricCollector.update(exchange, startTime);
            next.proceed();
        });
        return InterceptionDecision.CONTINUE;
    }
}
