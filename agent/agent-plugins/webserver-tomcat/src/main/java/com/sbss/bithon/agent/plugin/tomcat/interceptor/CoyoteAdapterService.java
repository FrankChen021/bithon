/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.tomcat.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
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
