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

package com.sbss.bithon.agent.plugin.jetty.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.web.RequestUriFilter;
import com.sbss.bithon.agent.core.metric.domain.web.UserAgentFilter;
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
