package com.sbss.bithon.agent.plugin.undertow;

import com.keruyun.commons.agent.collector.entity.RequestInfoEntity;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import com.sbss.bithon.agent.dispatcher.metrics.web.WebRequestMetrics;
import io.undertow.server.HttpServerExchange;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : Request记录器 <br>
 * Date: 17/10/30
 *
 * @author 马至远
 */
public class RequestCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(RequestCounter.class);
    /**
     * storage临时状态, 用于并发的统计
     */
    private WebRequestMetrics tempCounterStorage;

    /**
     * 请求数据存储
     */
    private Map<String, WebRequestMetrics> requests = new ConcurrentHashMap<>();

    RequestCounter() {
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
     * 将数据存储至Counter
     *
     * @param o 原始数据
     */
    private void createOrMerge(Object o) {
        BeforeJoinPoint beforeJoinPoint = (BeforeJoinPoint) o;

        if (beforeJoinPoint.getArgs().length > 0) {
            HttpServerExchange exchange = (HttpServerExchange) beforeJoinPoint.getTarget();
            //// TODO URL整形处理，注意处理以下"/;", "/?", "/&", "/."为"/"
            String uri = exchange.getRequestPath();
            int failureCount = exchange.getStatusCode() >= 400 ? 1 : 0;
            int httpStatus = exchange.getStatusCode();
            int failure40xCount = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
            int failure50xCount = httpStatus >= 500 && httpStatus < 600 ? 1 : 0;
            long requestByteSize = exchange.getRequestContentLength() < 0 ? 0 : exchange.getRequestContentLength();
            long responseByteSize = exchange.getResponseBytesSent();

            long costTime = System.nanoTime() - (Long) beforeJoinPoint.getArgs()[0];

            log.debug("undertow get failure40xCount {},failure50xCount {},requestByteSize {},responseByteSize {}",
                      failure40xCount,
                      failure50xCount,
                      requestByteSize,
                      responseByteSize);

            WebRequestMetrics webRequestMetrics = requests.computeIfAbsent(uri, WebRequestMetrics::new);
            webRequestMetrics.add(costTime, failureCount, failure40xCount, failure50xCount);
            webRequestMetrics.addByteSize(requestByteSize, responseByteSize);
        }
    }

    /**
     * 从当前storage中构建thrift数据
     *
     * @return agent采集数据
     */
    private List<RequestInfoEntity> buildEntities(int interval,
                                                  String appName,
                                                  String ipAddress,
                                                  int port) {
        List<RequestInfoEntity> requestInfoEntities = new ArrayList<>();

        for (Map.Entry<String, WebRequestMetrics> entry : requests.entrySet()) {
            requests.compute(entry.getKey(),
                             (k,
                              v) -> tempAndRemoveEntry(v));
            RequestInfoEntity requestInfoEntity = new RequestInfoEntity(appName,
                                                                        ipAddress,
                                                                        port,
                                                                        System.currentTimeMillis(),
                                                                        interval,
                                                                        null,
                                                                        tempCounterStorage.getFormattingRequestPerformance());
            requestInfoEntities.add(requestInfoEntity);
        }

        return requestInfoEntities;
    }

    private WebRequestMetrics tempAndRemoveEntry(WebRequestMetrics webRequestMetrics) {
        tempCounterStorage = webRequestMetrics;
        return null;
    }
}
