package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.IMetricCollector;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 2:37 下午
 */
@Slf4j
@Service
public class MetricCollectorThriftImpl implements IMetricCollector.Iface {

    private final JvmMetricMessageHandler jvmMetricMessageHandler;
    private final JvmGcMetricMessageHandler jvmGcMetricMessageHandler;
    private final WebRequestMetricMessageHandler webRequestMetricMessageHandler;
    private final WebServerMetricMessageHandler webServerMetricMessageHandler;
    private final ExceptionMetricMessageHandler exceptionMetricMessageHandler;
    private final HttpClientMetricMessageHandler httpClientMetricMessageHandler;
    private final ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler;
    private final JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler;
    private final RedisMetricMessageHandler redisMetricMessageHandler;

    public MetricCollectorThriftImpl(JvmMetricMessageHandler jvmMetricMessageHandler,
                                     JvmGcMetricMessageHandler jvmGcMetricMessageHandler,
                                     WebRequestMetricMessageHandler webRequestMetricMessageHandler,
                                     WebServerMetricMessageHandler webServerMetricMessageHandler,
                                     ExceptionMetricMessageHandler exceptionMetricMessageHandler,
                                     HttpClientMetricMessageHandler httpClientMetricMessageHandler,
                                     ThreadPoolMetricMessageHandler threadPoolMetricMessageHandler,
                                     JdbcPoolMetricMessageHandler jdbcPoolMetricMessageHandler,
                                     RedisMetricMessageHandler redisMetricMessageHandler) {
        this.jvmMetricMessageHandler = jvmMetricMessageHandler;
        this.jvmGcMetricMessageHandler = jvmGcMetricMessageHandler;
        this.webRequestMetricMessageHandler = webRequestMetricMessageHandler;
        this.webServerMetricMessageHandler = webServerMetricMessageHandler;
        this.exceptionMetricMessageHandler = exceptionMetricMessageHandler;
        this.httpClientMetricMessageHandler = httpClientMetricMessageHandler;
        this.threadPoolMetricMessageHandler = threadPoolMetricMessageHandler;
        this.jdbcPoolMetricMessageHandler = jdbcPoolMetricMessageHandler;
        this.redisMetricMessageHandler = redisMetricMessageHandler;
    }

    @Override
    public void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> message) {
        webRequestMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendJvm(MessageHeader header, List<JvmMetricMessage> message) {
        jvmMetricMessageHandler.submit(header, message);
        jvmGcMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendWebServer(MessageHeader header, List<WebServerMetricMessage> message) {
        webServerMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendException(MessageHeader header, List<ExceptionMetricMessage> message) {
        exceptionMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> message) {
        httpClientMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> message) {
        threadPoolMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> message) {
        jdbcPoolMetricMessageHandler.submit(header, message);
    }

    @Override
    public void sendRedis(MessageHeader header, List<RedisMetricMessage> message) {
        redisMetricMessageHandler.submit(header, message);
    }
}
