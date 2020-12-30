package com.sbss.bithon.collector.kafka.client;

import com.sbss.bithon.collector.intf.metrics.IMetricsCollector;
import com.sbss.bithon.collector.intf.metrics.jvm.JavaInstanceMetric;
import shaded.org.apache.kafka.clients.producer.KafkaProducer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29 10:26 下午
 */
public class KafkaClient implements IMetricsCollector {

    final KafkaProducer producer;

    public KafkaClient() {
        Map<String, Object> properties = new HashMap<>();
        this.producer = new KafkaProducer(properties);
    }

    @Override
    public void sendJavaInstanceMetric(JavaInstanceMetric metrics) {
    }

    @Override
    public void sendWebServerMetrics() {

    }
}
