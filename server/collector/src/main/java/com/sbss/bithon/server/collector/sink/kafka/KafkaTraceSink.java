package com.sbss.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.tracing.handler.TraceSpan;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaTraceSink implements IMessageSink<List<TraceSpan>> {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaTraceSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        try {
            producer.send(messageType,
                          spans.get(0).getInstanceName(),
                          objectMapper.writeValueAsString(spans));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            //TODO: log here
        }
    }
}
