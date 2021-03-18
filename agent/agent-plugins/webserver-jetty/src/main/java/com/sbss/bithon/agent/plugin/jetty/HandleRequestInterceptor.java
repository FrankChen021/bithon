package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.web.UserAgentFilter;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author frankchen
 */
public class HandleRequestInterceptor extends AbstractInterceptor {
    private static final String JETTY_REQUEST_BUFFER_MANAGER_NAME = "jetty-request";

    private RequestUriFilter uriFilter;
    private UserAgentFilter userAgentFilter;

    private WebRequestMetricCollector requestCounter;

    @Override
    public boolean initialize() {
        uriFilter = new RequestUriFilter();
        userAgentFilter = new UserAgentFilter();

        requestCounter = (WebRequestMetricCollector) MetricCollectorManager.getInstance()
                                                                           .register(JETTY_REQUEST_BUFFER_MANAGER_NAME,
                                                                                   new WebRequestMetricCollector());

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

        requestCounter.update((Request) context.getArgs()[1],
                              (HttpServletRequest) context.getArgs()[2],
                              (HttpServletResponse) context.getArgs()[3],
                              context.getCostTime());
    }
}
