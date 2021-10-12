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

package org.bithon.agent.plugin.jetty.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.plugin.jetty.metric.WebRequestMetricCollector;
import org.eclipse.jetty.server.Request;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author frankchen
 */
public class ContextHandlerDoHandle extends AbstractInterceptor {
    private HttpIncomingFilter requestFilter;

    private WebRequestMetricCollector requestMetricCollector;

    @Override
    public boolean initialize() {
        requestFilter = new HttpIncomingFilter();

        requestMetricCollector = MetricCollectorManager.getInstance()
                                                       .getOrRegister("jetty-web-request-metrics",
                                                                      WebRequestMetricCollector.class);

        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        Request request = (Request) context.getArgs()[1];
        boolean filtered = this.requestFilter.shouldBeExcluded(request.getRequestURI(), request.getHeader("User-Agent"));
        if (filtered) {
            return;
        }

        requestMetricCollector.update((Request) context.getArgs()[1],
                                      (HttpServletRequest) context.getArgs()[2],
                                      (HttpServletResponse) context.getArgs()[3],
                                      context.getCostTime());
    }
}
