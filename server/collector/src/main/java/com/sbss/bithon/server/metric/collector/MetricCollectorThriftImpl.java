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

    private final JvmMessageHandler jvmMessageHandler;
    private final JvmGcMessageHandler jvmGcMessageHandler;
    private final WebRequestMessageHandler webRequestMessageHandler;
    private final WebServerMessageHandler webServerMessageHandler;
    private final ExceptionMessageHandler exceptionMessageHandler;
    private final HttpClientMessageHandler httpClientMessageHandler;
    private final ThreadPoolMessageHandler threadPoolMessageHandler;
    private final JdbcPoolMessageHandler jdbcPoolMessageHandler;
    private final RedisMessageHandler redisMessageHandler;

    public MetricCollectorThriftImpl(JvmMessageHandler jvmMessageHandler,
                                     JvmGcMessageHandler jvmGcMessageHandler,
                                     WebRequestMessageHandler webRequestMessageHandler,
                                     WebServerMessageHandler webServerMessageHandler,
                                     ExceptionMessageHandler exceptionMessageHandler,
                                     HttpClientMessageHandler httpClientMessageHandler,
                                     ThreadPoolMessageHandler threadPoolMessageHandler,
                                     JdbcPoolMessageHandler jdbcPoolMessageHandler,
                                     RedisMessageHandler redisMessageHandler) {
        this.jvmMessageHandler = jvmMessageHandler;
        this.jvmGcMessageHandler = jvmGcMessageHandler;
        this.webRequestMessageHandler = webRequestMessageHandler;
        this.webServerMessageHandler = webServerMessageHandler;
        this.exceptionMessageHandler = exceptionMessageHandler;
        this.httpClientMessageHandler = httpClientMessageHandler;
        this.threadPoolMessageHandler = threadPoolMessageHandler;
        this.jdbcPoolMessageHandler = jdbcPoolMessageHandler;
        this.redisMessageHandler = redisMessageHandler;
    }

    @Override
    public void sendWebRequest(MessageHeader header, List<WebRequestMetricMessage> message) {
        webRequestMessageHandler.submit(header, message);
    }

    @Override
    public void sendJvm(MessageHeader header, List<JvmMetricMessage> message) {
        jvmMessageHandler.submit(header, message);
        jvmGcMessageHandler.submit(header, message);
    }

    @Override
    public void sendWebServer(MessageHeader header, List<WebServerMetricMessage> message) {
        webServerMessageHandler.submit(header, message);
    }

    @Override
    public void sendException(MessageHeader header, List<ExceptionMetricMessage> message) {
        exceptionMessageHandler.submit(header, message);
    }

    @Override
    public void sendHttpClient(MessageHeader header, List<HttpClientMetricMessage> message) {
        httpClientMessageHandler.submit(header, message);
    }

    @Override
    public void sendThreadPool(MessageHeader header, List<ThreadPoolMetricMessage> message) {
        threadPoolMessageHandler.submit(header, message);
    }

    @Override
    public void sendJdbc(MessageHeader header, List<JdbcPoolMetricMessage> message) {
        jdbcPoolMessageHandler.submit(header, message);
    }

    @Override
    public void sendRedis(MessageHeader header, List<RedisMetricMessage> message) {
        redisMessageHandler.submit(header, message);
    }
}
