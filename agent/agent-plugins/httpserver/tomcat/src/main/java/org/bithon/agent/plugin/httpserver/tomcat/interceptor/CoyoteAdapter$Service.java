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

package org.bithon.agent.plugin.httpserver.tomcat.interceptor;

import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingFilter;
import org.bithon.agent.observability.metric.domain.httpserver.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.propagation.ITracePropagator;

/**
 * Interceptor for {@link org.apache.catalina.connector.CoyoteAdapter#service(Request, Response)}
 *
 * @author frankchen
 */
public class CoyoteAdapter$Service extends AfterInterceptor {
    private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();
    private final HttpIncomingFilter requestFilter = new HttpIncomingFilter();

    @Override
    public void after(AopContext aopContext) {
        Request request = (Request) aopContext.getArgs()[0];
        String uri = request.requestURI().toString();

        //
        // Originally, this check is implemented in the Enter interceptor
        // However, on SpringBoot3 which uses tomcat 10.x, this causes application request failures
        // Still don't know why, but putting this check here as a post check helps resolve the problem.
        //
        if (requestFilter.shouldBeExcluded(uri,
                                           request.getHeader("User-Agent"))) {
            return;
        }

        Response response = aopContext.getArgAs(1);

        String srcApplication = request.getHeader(ITracePropagator.TRACE_HEADER_SRC_APPLICATION);

        int httpStatus = response.getStatus();
        int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
        int count5xx = httpStatus >= 500 ? 1 : 0;
        long requestByteSize = request.getBytesRead();
        long responseByteSize = response.getBytesWritten(false);

        this.metricRegistry.getOrCreateMetrics(srcApplication, request.method().toString(), uri, httpStatus)
                           .updateRequest(aopContext.getExecutionTime(), count4xx, count5xx)
                           .updateBytes(requestByteSize, responseByteSize);
    }
}
