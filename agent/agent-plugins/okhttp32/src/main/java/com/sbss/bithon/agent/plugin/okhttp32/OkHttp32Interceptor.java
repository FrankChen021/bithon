package com.sbss.bithon.agent.plugin.okhttp32;

import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.http.HttpClientMetricProvider;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
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

    private HttpClientMetricProvider dataSource;
    private Set<String> ignoredSuffixes;

    @Override
    public boolean initialize() {
        ignoredSuffixes = Arrays.stream("html, js, css, jpg, gif, png, swf, ttf, ico, woff, woff2, json, eot, svg".split(","))
            .map(x -> x.trim().toLowerCase())
            .collect(Collectors.toSet());

        dataSource = MetricProviderManager.getInstance().register("okhttp3.2", new HttpClientMetricProvider());

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
            dataSource.addExceptionRequest(requestUri,
                                           requestMethod,
                                           aopContext.getCostTime());
        } else {
            Response response = aopContext.castReturningAs();
            dataSource.addRequest(requestUri, requestMethod, response.code(), aopContext.getCostTime());
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

            dataSource.addBytes(uri, method, requestByteSize, responseByteSize);
        } catch (Exception e) {
            log.warn("OKHttp getByteSize", e);
        }
    }
}
