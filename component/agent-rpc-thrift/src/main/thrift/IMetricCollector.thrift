namespace java com.sbss.bithon.agent.rpc.thrift.service.metric

include "HeaderMessages.thrift"
include "MetricMessages.thrift"

service IMetricCollector {
    oneway void sendWebRequest(1:required HeaderMessages.MessageHeader header,
                               2:required list<MetricMessages.WebRequestMetricMessage> message);

    oneway void sendJvm(1:required HeaderMessages.MessageHeader header,
                        2:required list<MetricMessages.JvmMetricMessage> message);

    oneway void sendWebServer(1:required HeaderMessages.MessageHeader header,
                              2:required list<MetricMessages.WebServerMetricMessage> message);

    oneway void sendException(1:required HeaderMessages.MessageHeader header,
                              2:required list<MetricMessages.ExceptionMetricMessage> message);

    oneway void sendHttpClient(1:required HeaderMessages.MessageHeader header,
                               2:required list<MetricMessages.HttpClientMetricMessage> message);

    oneway void sendThreadPool(1:required HeaderMessages.MessageHeader header,
                               2:required list<MetricMessages.ThreadPoolMetricMessage> message);

    oneway void sendJdbc(1:required HeaderMessages.MessageHeader header,
                         2:required list<MetricMessages.JdbcPoolMetricMessage> message);

    oneway void sendRedis(1:required HeaderMessages.MessageHeader header,
                          2:required list<MetricMessages.RedisMetricMessage> message);
}
