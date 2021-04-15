/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbss.bithon.server.collector.sink.IMessageSink;
import com.sbss.bithon.server.common.utils.collection.CloseableIterator;
import com.sbss.bithon.server.metric.handler.GenericMetricMessage;
import com.sbss.bithon.server.tracing.handler.TraceSpan;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class KafkaTraceSink implements IMessageSink<CloseableIterator<TraceSpan>> {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaTraceSink(KafkaTemplate<String, String> producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, CloseableIterator<TraceSpan> spans) {
        if (!spans.hasNext()) {
            return;
        }

        //
        // a batch message in written into a single kafka message in which each text line is a single metric message
        //
        // of course we could also send messages in this batch one by one to Kafka,
        // but I don't think it has advantages over the way below
        //
        StringBuilder messageText = new StringBuilder();
        List<GenericMetricMessage> metricMessage = new ArrayList<>();
        while (spans.hasNext()) {
            try {
                messageText.append(objectMapper.writeValueAsString(spans.next()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            //it's not necessary, only used to improve readability of text when debugging
            messageText.append('\n');
        }

        producer.send(messageType,
                      // Sink receives messages from an agent, it's safe to use instance name of first item
                      metricMessage.get(0).getInstanceName(),
                      messageText.toString());
    }
}
