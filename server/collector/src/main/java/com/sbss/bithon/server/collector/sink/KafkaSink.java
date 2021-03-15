package com.sbss.bithon.server.collector.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaSink implements IMessageSink {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, Map<String, Object> message) {
        try {
            producer.send(messageType,
                    (String) message.get("instanceName"),
                    objectMapper.writeValueAsString(messageType));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //TODO: log here
        }
    }
}
