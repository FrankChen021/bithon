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

package org.bithon.agent.plugin.httpclient.okhttp39;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetrics;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingUriFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 3.9+ {@link okhttp3.RealCall()}
 * 4.4+ {@link okhttp3.internal.connection.RealCall()}
 *
 * @author frankchen
 */
public class RealCall$GetResponseWithInterceptorChain extends AroundInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();
    private final HttpOutgoingUriFilter filter = ConfigurationManager.getInstance().getConfig(HttpOutgoingUriFilter.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        Call realCall = aopContext.getTargetAs();
        Request request = realCall.request();
        String uri = request.url().uri().toString().split("\\?")[0];

        return needIgnore(uri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        Response response = aopContext.getReturningAs();
        Call realCall = (Call) aopContext.getTarget();
        Request request = realCall.request();

        String uri = request.url().uri().toString();
        String httpMethod = request.method().toUpperCase(Locale.ENGLISH);

        HttpOutgoingMetrics metrics;
        if (aopContext.getException() != null) {
            metrics = metricRegistry.addExceptionRequest(uri,
                                                         httpMethod,
                                                         aopContext.getExecutionTime());
        } else {
            metrics = metricRegistry.addRequest(uri, httpMethod, response.code(), aopContext.getExecutionTime());
        }

        RequestBody requestBody = request.body();
        if (requestBody != null) {
            try {
                long size = requestBody.contentLength();
                if (size > 0) {
                    metrics.getRequestBytes().update(size);
                }
            } catch (IOException ignored) {
            }
        }

        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            long size = responseBody.contentLength();
            if (size > 0) {
                metrics.getResponseBytes().update(size);
            }
        }
    }

    private boolean needIgnore(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ENGLISH);
        return filter.getSuffixes().contains(suffix);
    }
}
