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

package org.bithon.agent.plugin.httpclient.okhttp32;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.http.StreamAllocation;
import okhttp3.internal.io.RealConnection;
import okio.BufferedSource;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OkHttp3 agent plugin
 *
 * @author frankchen
 */
public class RealCall$GetResponseWithInterceptorChain extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(RealCall$GetResponseWithInterceptorChain.class);

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();
    private final Set<String> ignoredSuffixes = Arrays.stream("html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg".split(
                                                          ","))
                                                      .map(x -> x.trim().toLowerCase(Locale.ENGLISH))
                                                      .collect(Collectors.toSet());


    @Override
    public InterceptionDecision before(AopContext aopContext) {
        Call realCall = aopContext.getTargetAs();
        Request originRequest = realCall.request();
        String uri = originRequest.url().uri().toString().split("\\?")[0];
        return needIgnore(uri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        Call realCall = (Call) aopContext.getTarget();
        Request originRequest = realCall.request();

        String requestUri = originRequest.url().uri().toString();
        String requestMethod = originRequest.method().toUpperCase(Locale.ENGLISH);

        if (aopContext.getException() != null) {
            metricRegistry.addExceptionRequest(requestUri,
                                               requestMethod,
                                               aopContext.getExecutionTime());
        } else {
            Response response = aopContext.getReturningAs();
            metricRegistry.addRequest(requestUri, requestMethod, response.code(), aopContext.getExecutionTime());
        }

        addBytes(requestUri, requestMethod, originRequest, realCall);
    }

    private boolean needIgnore(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase(Locale.ENGLISH);
        return ignoredSuffixes.contains(suffix);
    }

    private void addBytes(String uri, String method,
                          Request request,
                          Call call) {
        try {
            long requestByteSize = 0;
            long responseByteSize = 0;
            if (request.body() != null) {
                requestByteSize = request.body().contentLength() < 0 ? 0 : request.body().contentLength();
            }

            StreamAllocation streamAllocation = Internal.instance.callEngineGetStreamAllocation(call);
            RealConnection realConnection = streamAllocation.connection();
            if (realConnection != null) {
                responseByteSize = ((BufferedSource) ReflectionUtils.getFieldValue(realConnection, "source")).buffer()
                                                                                                             .size();
            }

            metricRegistry.addBytes(uri, method, requestByteSize, responseByteSize);
        } catch (Exception e) {
            log.warn("OKHttp getByteSize", e);
        }
    }
}
