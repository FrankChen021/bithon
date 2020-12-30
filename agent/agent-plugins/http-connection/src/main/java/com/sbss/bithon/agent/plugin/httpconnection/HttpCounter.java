package com.sbss.bithon.agent.plugin.httpconnection;

import com.keruyun.commons.agent.collector.entity.HttpEntity;
import com.keruyun.commons.agent.collector.enums.HttpMethodEnum;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import com.sbss.bithon.agent.dispatcher.metrics.http.HttpMetrics;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : http client- http connection记录器 <br>
 * Date: 17/10/30 TODO 注意几个HTTP client组件的重用
 *
 * @author 马至远
 */
public class HttpCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(HttpCounter.class);

    static final String KEY_BEGIN_TIME = "beginProcessingTime";

    static final String COUNTER_NAME = "http-connection";

    private static final int FAILURE_CODE = 400;
    private static final int ERROR_CODE = 500;

    /**
     * storage临时状态, 用于并发的统计
     */
    private HttpMetrics tempCounterStorage;

    /**
     * 请求数据存储
     */
    private Map<String, HttpMetrics> requests = new ConcurrentHashMap<>();

    HttpCounter() {
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
    private void createOrMerge(Object o) {
        try {
            AfterJoinPoint afterJoinPoint = (AfterJoinPoint) o;
            long costTime = System.nanoTime() - (Long) afterJoinPoint.getContext();

            HttpURLConnection httpUrlConnection = (HttpURLConnection) afterJoinPoint.getTarget();
            // TODO URL整形处理，注意处理以下"/;", "/?", "/&", "/."为"/"
            String uri = httpUrlConnection.getURL().toExternalForm().split("\\?")[0];
            String method = httpUrlConnection.getRequestMethod();
            String requestId = uri.concat(method);

            for (HttpMethodEnum httpMethodEnum : HttpMethodEnum.values()) {
                if (httpMethodEnum.toString().toLowerCase().equals(method.toLowerCase())) {
                    method = String.valueOf(httpMethodEnum.getValue());
                    break;
                }
            }

            String realMethod = method;

            int statusCode = httpUrlConnection.getResponseCode();
            int failureCount = 0, errorCount = 0;
            if (statusCode >= FAILURE_CODE) {
                if (statusCode >= ERROR_CODE) {
                    errorCount++;
                } else {
                    failureCount++;
                }
            }

            HttpMetrics httpMetrics = requests.computeIfAbsent(requestId,
                                                               k -> new HttpMetrics(uri,
                                                                                    realMethod));
            httpMetrics.add(costTime, failureCount, errorCount);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
                                                   String.valueOf(2),
                                                   tempCounterStorage.getRequestCount(),
                                                   0L,
                                                   0L);
            httpEntities.add(httpEntity);
        }

        return httpEntities;
    }

    private HttpMetrics tempAndRemoveEntry(HttpMetrics httpMetrics) {
        tempCounterStorage = httpMetrics;
        return null;
    }
}
