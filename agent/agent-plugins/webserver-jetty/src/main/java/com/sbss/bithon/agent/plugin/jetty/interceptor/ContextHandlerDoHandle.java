package com.sbss.bithon.agent.plugin.jetty.interceptor;

import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.plugin.jetty.metric.WebRequestMetricCollector;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author frankchen
 */
public class ContextHandlerDoHandle extends AbstractInterceptor {
    private RequestUriFilter uriFilter;
    private UserAgentFilter userAgentFilter;

    private WebRequestMetricCollector requestMetricCollector;

    @Override
    public boolean initialize() {
        uriFilter = new RequestUriFilter();
        userAgentFilter = new UserAgentFilter();

        requestMetricCollector = MetricCollectorManager.getInstance()
                                                       .getOrRegister("jetty-web-request-metrics",
                                                                      WebRequestMetricCollector.class);

        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        Request request = (Request) context.getArgs()[1];
        boolean filtered = this.userAgentFilter.isFiltered(request.getHeader("User-Agent"))
                           || this.uriFilter.isFiltered(request.getRequestURI());
        if (filtered) {
            return;
        }

        requestMetricCollector.update((Request) context.getArgs()[1],
                                      (HttpServletRequest) context.getArgs()[2],
                                      (HttpServletResponse) context.getArgs()[3],
                                      context.getCostTime());
    }
}
