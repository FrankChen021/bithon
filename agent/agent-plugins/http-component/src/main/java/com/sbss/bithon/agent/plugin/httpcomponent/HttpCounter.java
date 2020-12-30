package com.sbss.bithon.agent.plugin.httpcomponent;

import com.keruyun.commons.agent.collector.entity.HttpEntity;
import com.keruyun.commons.agent.collector.enums.HttpMethodEnum;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import com.sbss.bithon.agent.dispatcher.metrics.http.HttpMetrics;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.impl.client.DefaultRequestDirector;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.impl.execchain.MinimalClientExec;
import org.apache.http.impl.execchain.RedirectExec;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : http client - apache http component记录器 <br>
 * Date: 17/10/30 TODO 注意几个HTTP client组件的重用
 *
 * @author 马至远
 */

public class HttpCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(HttpCounter.class);

    static final String KEY_BEGIN_TIME = "beginProcessingTime";

    private static final String COUNTER_NAME = "http-component";

    private boolean isNewVersion = true;

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

    private HttpCounter() {
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();
        try {
            counterRepository.register(COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("apache http component counter initial failed due to ", e);
        }

        try {
            Class.forName("org.apache.http.impl.execchain.MinimalClientExec");
        } catch (ClassNotFoundException e) {
            isNewVersion = false;
        }
    }

    private static class HttpCounterHolder {
        static final HttpCounter INSTANCE = new HttpCounter();
    }

    static HttpCounter getInstance() {
        return HttpCounterHolder.INSTANCE;
    }

    void setIgnoredSuffixes(Set<String> ignoredSuffixes) {
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
    public List buildAndGetThriftEntities(int interval,
                                          String appName,
                                          String ipAddress,
                                          int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    /**
     * 将数据存储至Counter
     *
     * @param o 原始数据
     */
    @SuppressWarnings("deprecation")
    private void createOrMerge(Object o) {
        AfterJoinPoint afterJoinPoint = (AfterJoinPoint) o;
        long costTime = System.nanoTime() - (Long) afterJoinPoint.getContext();

        Object[] args = afterJoinPoint.getArgs();
        Object result = afterJoinPoint.getResult();
        Object target = afterJoinPoint.getTarget();
        boolean gotException = false;

        if (afterJoinPoint.getException() != null) {
            gotException = true;
        }

        if (target instanceof DefaultRequestDirector) {
            log.debug("old http client 4.0.1~4.2.5");
            countOldVersionClientData(args, result, costTime, gotException);
        } else if (isNewVersion && target instanceof MinimalClientExec) {
            log.debug("http client 4.3.4~4.5.3: MinimalClientExec");
            countMinimalClientData(args, result, costTime, gotException);
        } else if (isNewVersion && target instanceof RedirectExec) {
            log.debug("http client 4.3.4~4.5.3: RedirectExec");
            countInternalClientData(args, costTime, gotException);
        } else if (isNewVersion) {
            log.debug("http client 4.3.4~4.5.3: InternalHttpClient");
            countInternalClientData(args, result, costTime, gotException);
        } else {
            log.warn("http client version not supported!");
        }
    }

    private void countAnErrorRequest(HttpRequest httpRequest,
                                     long costTime) {
        String uri = httpRequest.getRequestLine().getUri().split("\\?")[0];
        if (needIgnore(uri)) {
            return;
        }

        String requestMethod = getThriftWantedRequestMethod(httpRequest);
        String requestId = uri.concat("|").concat(requestMethod);

        HttpMetrics httpMetrics = requests.computeIfAbsent(requestId,
                                                           k -> new HttpMetrics(uri,
                                                                                requestMethod));
        httpMetrics.add(costTime, 0, 1);
    }

    private void countInternalClientData(Object[] args,
                                         Object result,
                                         long costTime,
                                         boolean gotException) {
        HttpRequest httpRequest = (HttpRequest) args[1];

        if (gotException) {
            countAnErrorRequest(httpRequest, costTime);
            return;
        }

        HttpResponse httpResponse = (HttpResponse) result;
        countBasicHttpClientInfo(httpRequest, httpResponse, costTime);
    }

    private void countOldVersionClientData(Object[] args,
                                           Object result,
                                           long costTime,
                                           boolean gotException) {
        HttpRequest httpRequest = (HttpRequest) args[1];
        if (gotException) {
            countAnErrorRequest(httpRequest, costTime);
            return;
        }
        HttpContext httpContext = (HttpContext) args[2];
        HttpResponse httpResponse = (HttpResponse) result;

        countBasicHttpClientInfo(httpRequest, httpResponse, costTime);
        countHttpClientBytesInfo(httpRequest, httpContext);
    }

    private void countMinimalClientData(Object[] args,
                                        Object result,
                                        long costTime,
                                        boolean gotException) {
        HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) args[1];
        if (gotException) {
            countAnErrorRequest(httpRequestWrapper, costTime);
            return;
        }
        HttpContext httpContext = (HttpContext) args[2];
        HttpResponse httpResponse = (HttpResponse) result;

        countBasicHttpClientInfo(httpRequestWrapper, httpResponse, costTime);
        countHttpClientBytesInfo(httpRequestWrapper, httpContext);
    }

    private void countInternalClientData(Object[] args,
                                         long costTime,
                                         boolean gotException) {
        HttpRequestWrapper httpRequestWrapper = (HttpRequestWrapper) args[1];
        HttpRequest originalRequest = httpRequestWrapper.getOriginal();
        if (gotException) {
            countAnErrorRequest(httpRequestWrapper, costTime);
            return;
        }

        HttpContext httpContext = (HttpContext) args[2];
        countHttpClientBytesInfo(originalRequest, httpContext);
    }

    private boolean needIgnore(String uri) {
        String suffix = uri.substring(uri.lastIndexOf(".") + 1).toLowerCase();
        return ignoredSuffixes.contains(suffix);
    }

    private void countBasicHttpClientInfo(HttpRequest httpRequest,
                                          HttpResponse httpResponse,
                                          long costTime) {
        int response4xxCount = 0, response5xxCount = 0;

        String uri = httpRequest.getRequestLine().getUri().split("\\?")[0];
        if (needIgnore(uri)) {
            return;
        }

        String requestMethod = getThriftWantedRequestMethod(httpRequest);

        int responseCode = httpResponse.getStatusLine().getStatusCode();

        if (responseCode >= RESPONSE_400_CODE) {
            if (responseCode >= RESPONSE_500_CODE) {
                response5xxCount++;
            } else {
                response4xxCount++;
            }
        }

        String requestId = uri.concat("|").concat(requestMethod);
        log.debug("-- basic requestId=" + requestId);

        HttpMetrics httpMetrics = requests.computeIfAbsent(requestId,
                                                           k -> new HttpMetrics(uri,
                                                                                requestMethod));
        httpMetrics.add(costTime, response4xxCount, response5xxCount);
    }

    private String getThriftWantedRequestMethod(HttpRequest httpRequest) {
        String requestMethod = httpRequest.getRequestLine().getMethod();

        for (HttpMethodEnum httpMethodEnum : HttpMethodEnum.values()) {
            if (httpMethodEnum.toString().equalsIgnoreCase(requestMethod)) {
                requestMethod = String.valueOf(httpMethodEnum.getValue());
                break;
            }
        }
        return requestMethod;
    }

    private void countHttpClientBytesInfo(HttpRequest httpRequest,
                                          HttpContext httpContext) {
        if (httpContext == null || httpContext.getAttribute("http.connection") == null) {
            return;
        }
        HttpConnection basicPooledConnAdapter = (HttpConnection) httpContext.getAttribute("http.connection");
        try {
            String uri = httpRequest.getRequestLine().getUri().split("\\?")[0];
            if (needIgnore(uri)) {
                return;
            }
            String requestMethod = getThriftWantedRequestMethod(httpRequest);

            HttpConnectionMetrics connMetrics = basicPooledConnAdapter.getMetrics();

            long requestByteSize = connMetrics.getSentBytesCount();
            long responseByteSize = connMetrics.getReceivedBytesCount();

            log.debug("httpClient requestByteSize {},request content_length {},responseByteSize",
                      requestByteSize,
                      httpRequest.getFirstHeader(HTTP.CONTENT_LEN),
                      responseByteSize);

            String requestId = uri.concat("|").concat(requestMethod);
            log.debug("-- byte requestId=" + requestId + ", in=" + responseByteSize + ", out=" + requestByteSize);
            HttpMetrics httpMetrics = requests.computeIfAbsent(requestId,
                                                               k -> new HttpMetrics(uri,
                                                                                    requestMethod));
            httpMetrics.addByteSize(requestByteSize, responseByteSize);
        } catch (ConnectionShutdownException e) {
            log.debug("apache httpClient connection shutdown when reading metric");
        }
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
                                                   String.valueOf(1),
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
