package com.sbss.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.common.utils.collection.SizedIterator;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaMetricSink implements IMessageSink<SizedIterator<GenericMetricMessage>> {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaMetricSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, SizedIterator<GenericMetricMessage> messages) {
        try {
            List<GenericMetricMessage> metricMessage = new ArrayList<>();
            while (messages.hasNext()) {
                metricMessage.add(messages.next());
            }
            producer.send(messageType,
                          metricMessage.get(0).getInstanceName(),
                          objectMapper.writeValueAsString(metricMessage));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //TODO: log here
        }
    }
}
