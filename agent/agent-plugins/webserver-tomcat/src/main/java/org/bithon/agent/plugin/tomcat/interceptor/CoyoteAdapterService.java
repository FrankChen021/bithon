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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.metric.domain.web.HttpIncomingFilter;
import org.bithon.agent.core.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.core.tracing.propagation.ITracePropagator;

/**
 * @author frankchen
 */
public class CoyoteAdapterService extends AbstractInterceptor {
    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();
    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Request request = (Request) aopContext.getArgs()[0];
        if (requestFilter.shouldBeExcluded(request.requestURI().toString(), request.getHeader("User-Agent"))) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        return super.onMethodEnter(aopContext);
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[0];

        update(request,
               (Response) aopContext.getArgs()[1],
               aopContext.getCostTime());
    }

    private void update(Request request, Response response, long responseTime) {
        String uri = request.requestURI().toString();
        if (uri == null) {
            return;
        }

        String srcApplication = request.getHeader(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);

        int httpStatus = response.getStatus();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = request.getBytesRead();
        long responseByteSize = response.getBytesWritten(false);

        this.metricRegistry.getOrCreateMetrics(srcApplication, request.method().toString(), uri, httpStatus)
                           .updateRequest(responseTime, count4xx, count5xx)
                           .updateBytes(requestByteSize, responseByteSize);
    }
}
