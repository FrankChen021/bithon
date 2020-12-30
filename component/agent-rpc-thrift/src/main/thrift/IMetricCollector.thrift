namespace java com.sbss.bithon.agent.rpc.thrift.service.metric

include "MetricMessages.thrift"

service IMetricCollector {
    oneway void sendWebRequest(1:required MetricMessages.WebRequestMessage message);

    oneway void sendJvm(1:required MetricMessages.JvmMessage message);

    oneway void sendWebServer(1:required MetricMessages.WebServerMessage message);

    oneway void sendException(1:required MetricMessages.ExceptionMessage message);

    oneway void sendHttpClient(1:required MetricMessages.HttpClientMessage message);

    oneway void sendThreadPool(1:required MetricMessages.ThreadPoolMessage message);

    oneway void sendJdbc(1:required MetricMessages.JdbcMessage message);

    oneway void sendRedis(1:required MetricMessages.RedisMessage message);
}
