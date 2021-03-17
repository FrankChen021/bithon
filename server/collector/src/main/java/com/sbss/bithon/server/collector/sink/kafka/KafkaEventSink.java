package com.sbss.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.events.handler.EventMessage;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaEventSink implements IMessageSink<EventMessage> {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaEventSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, EventMessage event) {
        try {
            producer.send(messageType,
                          event.getInstanceName(),
                          objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //TODO: log here
        }
    }
}
