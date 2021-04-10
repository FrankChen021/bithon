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

package com.sbss.bithon.agent.plugin.httpclient.okhttp32;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.InterceptionDecision;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.http.HttpClientMetricCollector;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Internal;
import okhttp3.internal.http.StreamAllocation;
import okhttp3.internal.io.RealConnection;
import okio.BufferedSource;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OkHttp3 agent plugin
 *
 * @author frankchen
 */
public class OkHttp32Interceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(OkHttp32Interceptor.class);

    private HttpClientMetricCollector metricCollector;
    private Set<String> ignoredSuffixes;

    @Override
    public boolean initialize() {
        ignoredSuffixes = Arrays.stream("html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg".split(
            ","))
                                .map(x -> x.trim().toLowerCase())
                                .collect(Collectors.toSet());

        metricCollector = MetricCollectorManager.getInstance()
                                                .getOrRegister("okhttp3.2", HttpClientMetricCollector.class);

        return true;
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Call realCall = aopContext.castTargetAs();
        Request originRequest = realCall.request();
        String uri = originRequest.url().uri().toString().split("\\?")[0];
        return needIgnore(uri) ? InterceptionDecision.SKIP_LEAVE : InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Call realCall = (Call) aopContext.getTarget();
        Request originRequest = realCall.request();

        String requestUri = originRequest.url().uri().toString();
        String requestMethod = originRequest.method().toUpperCase();

        if (aopContext.getException() != null) {
            metricCollector.addExceptionRequest(requestUri,
                                                requestMethod,
                                                aopContext.getCostTime());
        } else {
            Response response = aopContext.castReturningAs();
            metricCollector.addRequest(requestUri, requestMethod, response.code(), aopContext.getCostTime());
        }

        addBytes(requestUri, requestMethod, originRequest, realCall);
    }

    private boolean needIgnore(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
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

            metricCollector.addBytes(uri, method, requestByteSize, responseByteSize);
        } catch (Exception e) {
            log.warn("OKHttp getByteSize", e);
        }
    }
}
