package com.sbss.bithon.server.collector.kafka;

import java.util.Map;

/**
 * A Kafka Collector is a connector connecting to a KafkaSink
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public interface IKafkaCollector {
    IKafkaCollector start(Map<String, Object> consumerProps);

    void stop();

    boolean isRunning();
}
