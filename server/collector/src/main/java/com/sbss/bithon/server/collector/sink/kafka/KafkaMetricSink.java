package com.sbss.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.collector.GenericMessage;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaMetricSink implements IMessageSink<GenericMessage> {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaMetricSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, GenericMessage message) {
        try {
            producer.send(messageType,
                    (String) message.get("instanceName"),
                    objectMapper.writeValueAsString(message.getValues()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //TODO: log here
        }
    }
}
