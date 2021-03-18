package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.core.metric.MetricProviderManager;
import com.sbss.bithon.agent.core.metric.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.web.UserAgentFilter;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
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
        metricProvider = MetricProviderManager.getInstance()
                                              .register(TOMCAT_REQUEST_BUFFER_MANAGER_NAME,
                                                        new WebRequestMetricProvider());

        uriFilter = new RequestUriFilter();
        userAgentFilter = new UserAgentFilter();

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Request request = (Request) aopContext.getArgs()[0];
        if (userAgentFilter.isFiltered(request.getHeader("User-Agent"))
            || uriFilter.isFiltered(request.requestURI().toString())) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        return super.onMethodEnter(aopContext);
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[0];
        if (userAgentFilter.isFiltered(request.getHeader("User-Agent"))
            || uriFilter.isFiltered(request.requestURI().toString())) {
            return;
        }

        metricProvider.update(request,
                              (Response) aopContext.getArgs()[1],
                              aopContext.getCostTime());
    }
}
