package com.sbss.bithon.agent.plugin.okhttp3;

import com.keruyun.commons.agent.collector.entity.HttpEntity;
import com.keruyun.commons.agent.collector.enums.HttpMethodEnum;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.util.ReflectUtil;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import com.sbss.bithon.agent.dispatcher.metrics.http.HttpMetrics;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.http.RetryAndFollowUpInterceptor;
import okio.BufferedSource;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : http客户端 - okhttp3记录器 <br>
 * Date: 17/10/30 TODO 注意几个HTTP client组件的重用
 *
 * @author 马至远
 */
public class HttpCounter implements IAgentCounter {

    private static final Logger log = LoggerFactory.getLogger(HttpCounter.class);
    static final String KEY_BEGIN_TIME = "beginProcessingTime";

    static final String COUNTER_NAME = "okhttp3";

    private static final int RESPONSE_400_CODE = 400;
    private static final int RESPONSE_500_CODE = 500;

    private Set<String> ignoredSuffixes;

    /**
     * storage临时状态, 用于并发的统计
     */
    private HttpMetrics tempCounterStorage;

    /**
     * 请求数据存储
     */
    private Map<String, HttpMetrics> requests = new ConcurrentHashMap<>();

    HttpCounter(Set<String> ignoredSuffixes) {
        this.ignoredSuffixes = ignoredSuffixes;
    }

    @Override
    public void add(Object o) {
        createOrMerge(o);
    }

    @Override
    public boolean isEmpty() {
        return requests.isEmpty();
    }

    @Override
    public List<?> buildAndGetThriftEntities(int interval,
                                             String appName,
                                             String ipAddress,
                                             int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    /**
     * 将数据存储至Counter TODO 代码重复注意重用
     *
     * @param o 原始数据
     */
    private void createOrMerge(Object o) {
        AfterJoinPoint afterJoinPoint = (AfterJoinPoint) o;
        long costTime = System.nanoTime() - (Long) afterJoinPoint.getContext();

        Call realCall = (Call) afterJoinPoint.getTarget();
        Request originRequest = realCall.request();
        Response response = (Response) afterJoinPoint.getResult();
        // TODO URL整形处理，注意处理以下"/;", "/?", "/&", "/."为"/"
        String uri = originRequest.url().uri().toString().split("\\?")[0];
        if (needIgnore(uri)) {
            return;
        }
        String method = originRequest.method();
        String requestId = uri.concat(method);

        for (HttpMethodEnum httpMethodEnum : HttpMethodEnum.values()) {
            if (httpMethodEnum.toString().toLowerCase().equals(method.toLowerCase())) {
                method = String.valueOf(httpMethodEnum.getValue());
                break;
            }
        }
        int failureCount = 0, errorCount = 0;

        String realMethod = method;

        if (afterJoinPoint.getException() != null) {
            errorCount++;
        } else {
            int statusCode = response.code();
            if (statusCode >= RESPONSE_400_CODE) {
                if (statusCode >= RESPONSE_500_CODE) {
                    errorCount++;
                } else {
                    failureCount++;
                }
            }
        }

        HttpMetrics httpMetrics = requests.computeIfAbsent(requestId,
                                                           k -> new HttpMetrics(uri, realMethod));
        httpMetrics.add(costTime, failureCount, errorCount);

        this.getByteSize(originRequest, response, httpMetrics, realCall);
    }

    private void getByteSize(Request originRequest,
                             Response response,
                             HttpMetrics httpMetrics,
                             Call call) {
        try {

            log.debug("okhttp 3.4 getByteSize");

            long requestByteSize = 0;
            long responseByteSize = 0;
            if (originRequest.body() != null) {
                requestByteSize = originRequest.body().contentLength() < 0 ? 0 : originRequest.body().contentLength();
            }

            RealConnection realConnection = null;
            // 3.4
            RetryAndFollowUpInterceptor retryAndFollowUpInterceptor = (RetryAndFollowUpInterceptor) ReflectUtil.getFieldValue(call,
                                                                                                                              "retryAndFollowUpInterceptor");
            if (retryAndFollowUpInterceptor != null) {
                realConnection = retryAndFollowUpInterceptor.streamAllocation().connection();
            } else {
                // 3.2.0
                // Object HttpEngine = ReflectUtil.getFieldValue(call, "engine");
                // StreamAllocation streamAllocation = (StreamAllocation)
                // ReflectUtil.getFieldValue(HttpEngine, "streamAllocation");
                // realConnection = streamAllocation.connection();
            }

            if (realConnection != null) {
                responseByteSize = ((BufferedSource) ReflectUtil.getFieldValue(realConnection, "source")).buffer()
                    .size();
            }

            log.debug("OKHttp ,request requestByteSize {},responseByteSize {}", requestByteSize, responseByteSize);

            httpMetrics.addByteSize(requestByteSize, responseByteSize);
        } catch (Exception e) {
            log.error("OKHttp getByteSize", e);
        }
    }

    private boolean needIgnore(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return ignoredSuffixes.contains(suffix);
    }

    /**
     * 从当前storage中构建thrift数据
     *
     * @return agent采集数据
     */
    private List<HttpEntity> buildEntities(int interval,
                                           String appName,
                                           String ipAddress,
                                           int port) {
        List<HttpEntity> httpEntities = new ArrayList<>();

        for (Map.Entry<String, HttpMetrics> entry : requests.entrySet()) {
            requests.compute(entry.getKey(),
                             (k,
                              v) -> tempAndRemoveEntry(v));
            HttpEntity httpEntity = new HttpEntity(appName,
                                                   ipAddress,
                                                   port,
                                                   System.currentTimeMillis(),
                                                   interval,
                                                   null,
                                                   tempCounterStorage.getUri(),
                                                   tempCounterStorage.getMethod(),
                                                   tempCounterStorage.getCostTime(),
                                                   tempCounterStorage.getFailureCount(),
                                                   tempCounterStorage.getErrorCount(),
                                                   String.valueOf(0),
                                                   tempCounterStorage.getRequestCount(),
                                                   tempCounterStorage.getRequestByteSize(),
                                                   tempCounterStorage.getResponseByteSize());
            httpEntities.add(httpEntity);
        }

        return httpEntities;
    }

    private HttpMetrics tempAndRemoveEntry(HttpMetrics httpMetrics) {
        tempCounterStorage = httpMetrics;
        return null;
    }

}
