namespace java com.sbss.bithon.agent.rpc.thrift.service.metric

include "HeaderMessages.thrift"
include "MetricMessages.thrift"

service IMetricCollector {
    oneway void sendWebRequest(1:required HeaderMessages.MessageHeader header,
                               2:required list<MetricMessages.WebRequestMetricMessage> messages);

    oneway void sendJvm(1:required HeaderMessages.MessageHeader header,
                        2:required list<MetricMessages.JvmMetricMessage> messages);

    oneway void sendWebServer(1:required HeaderMessages.MessageHeader header,
                              2:required list<MetricMessages.WebServerMetricMessage> messages);

    oneway void sendException(1:required HeaderMessages.MessageHeader header,
                              2:required list<MetricMessages.ExceptionMetricMessage> messages);

    oneway void sendHttpClient(1:required HeaderMessages.MessageHeader header,
                               2:required list<MetricMessages.HttpClientMetricMessage> messages);

    oneway void sendThreadPool(1:required HeaderMessages.MessageHeader header,
                               2:required list<MetricMessages.ThreadPoolMetricMessage> messages);

    oneway void sendJdbc(1:required HeaderMessages.MessageHeader header,
                         2:required list<MetricMessages.JdbcPoolMetricMessage> messages);

    oneway void sendRedis(1:required HeaderMessages.MessageHeader header,
                          2:required list<MetricMessages.RedisMetricMessage> messages);

    oneway void sendSql(1:required HeaderMessages.MessageHeader header,
                        2:required list<MetricMessages.SqlMetricMessage> messages);

    oneway void sendMongoDb(1:required HeaderMessages.MessageHeader header,
                            2:required list<MetricMessages.MongoDbMetricMessage> messages);
}
