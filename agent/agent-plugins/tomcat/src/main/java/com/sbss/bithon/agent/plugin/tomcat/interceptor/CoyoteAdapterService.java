package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metrics.web.UserAgentFilter;
import com.sbss.bithon.agent.plugin.tomcat.metric.WebRequestMetricProvider;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

/**
 * @author frankchen
 */
public class CoyoteAdapterService extends AbstractInterceptor {
    private static final String TOMCAT_REQUEST_BUFFER_MANAGER_NAME = "tomcat-request";

    private WebRequestMetricProvider metricProvider;
    private RequestUriFilter uriFilter;
    private UserAgentFilter userAgentFilter;

    @Override
    public boolean initialize() {
        metricProvider = MetricProviderManager.getInstance().register(TOMCAT_REQUEST_BUFFER_MANAGER_NAME, new WebRequestMetricProvider());

        uriFilter = new RequestUriFilter();
        userAgentFilter = new UserAgentFilter();

        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        Request request = (Request) context.getArgs()[0];
        if (userAgentFilter.isFiltered(request.getHeader("User-Agent"))
            || uriFilter.isFiltered(request.requestURI().toString())) {
            return;
        }

        metricProvider.update(request,
                              (Response) context.getArgs()[1],
                              context.getCostTime());
    }
}
