package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.plugin.tomcat.metric.WebRequestMetricCollector;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

/**
 * @author frankchen
 */
public class CoyoteAdapterService extends AbstractInterceptor {
    private WebRequestMetricCollector metricCollector;
    private RequestUriFilter uriFilter;
    private UserAgentFilter userAgentFilter;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("tomcat-web-request-metrics",
                                                               WebRequestMetricCollector.class);

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

        metricCollector.update(request,
                               (Response) aopContext.getArgs()[1],
                               aopContext.getCostTime());
    }
}
