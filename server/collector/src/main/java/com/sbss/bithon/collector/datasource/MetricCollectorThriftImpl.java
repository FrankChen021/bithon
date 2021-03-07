package com.sbss.bithon.collector.datasource;

import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.*;
import com.sbss.bithon.collector.common.message.handlers.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
@Service
public class MetricCollectorThriftImpl implements IMetricCollector.Iface {

    private final JvmMessageHandler jvmMessageHandler;
    private final JvmGcMessageHandler jvmGcMessageHandler;
    private final WebRequestMessageHandler webRequestMessageHandler;
    private final WebServerMessageHandler webServerMessageHandler;
    private final ExceptionMessageHandler exceptionMessageHandler;
    private final HttpClientMessageHandler httpClientMessageHandler;
    private final ThreadPoolMessageHandler threadPoolMessageHandler;
    private final JdbcMessageHandler jdbcMessageHandler;
    private final RedisMessageHandler redisMessageHandler;

    public MetricCollectorThriftImpl(JvmMessageHandler jvmMessageHandler,
                                     JvmGcMessageHandler jvmGcMessageHandler,
                                     WebRequestMessageHandler webRequestMessageHandler,
                                     WebServerMessageHandler webServerMessageHandler,
                                     ExceptionMessageHandler exceptionMessageHandler,
                                     HttpClientMessageHandler httpClientMessageHandler,
                                     ThreadPoolMessageHandler threadPoolMessageHandler,
                                     JdbcMessageHandler jdbcMessageHandler,
                                     RedisMessageHandler redisMessageHandler) {
        this.jvmMessageHandler = jvmMessageHandler;
        this.jvmGcMessageHandler = jvmGcMessageHandler;
        this.webRequestMessageHandler = webRequestMessageHandler;
        this.webServerMessageHandler = webServerMessageHandler;
        this.exceptionMessageHandler = exceptionMessageHandler;
        this.httpClientMessageHandler = httpClientMessageHandler;
        this.threadPoolMessageHandler = threadPoolMessageHandler;
        this.jdbcMessageHandler = jdbcMessageHandler;
        this.redisMessageHandler = redisMessageHandler;
    }

    @Override
    public void sendWebRequest(WebRequestMessage message) {
        webRequestMessageHandler.submit(message);
    }

    @Override
    public void sendJvm(JvmMessage message) {
        jvmMessageHandler.submit(message);
        jvmGcMessageHandler.submit(message);
    }

    @Override
    public void sendWebServer(WebServerMessage message) {
        webServerMessageHandler.submit(message);
    }

    @Override
    public void sendException(ExceptionMessage message) {
        exceptionMessageHandler.submit(message);
    }

    @Override
    public void sendHttpClient(HttpClientMessage message) {
        httpClientMessageHandler.submit(message);
    }

    @Override
    public void sendThreadPool(ThreadPoolMessage message) {
        threadPoolMessageHandler.submit(message);
    }

    @Override
    public void sendJdbc(JdbcMessage message) {
        jdbcMessageHandler.submit(message);
    }

    @Override
    public void sendRedis(RedisMessage message) {
        redisMessageHandler.submit(message);
    }
}
