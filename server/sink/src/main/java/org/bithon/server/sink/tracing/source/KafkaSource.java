/*
 *    Copyright 2020 bithon.org
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

package org.bithon.server.sink.tracing.source;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bithon.server.kafka.AbstractKafkaConsumer;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
@JsonTypeName("kafka")
public class KafkaSource extends AbstractKafkaConsumer implements ITraceMessageSource {
    private final Map<String, Object> props;
    private ITraceMessageSink processor;
    private final TypeReference<List<TraceSpan>> typeReference;

    public KafkaSource(@JacksonInject(useInput = OptBoolean.FALSE) Map<String, Object> props,
                       @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        super(applicationContext);

        this.typeReference = new TypeReference<List<TraceSpan>>() {
        };
        this.props = props;
    }

    @Override
    public void start() {
        this.start(props);
    }

    @Override
    public void registerProcessor(ITraceMessageSink processor) {
        this.processor = processor;
    }

    @Override
    public void stop() {
        // stop receiving
        try {
            super.stop();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onMessage(List<ConsumerRecord<String, byte[]>> records) {
        if (this.processor == null) {
            return;
        }

        List<TraceSpan> spans = new ArrayList<>(16);

        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                List<TraceSpan> msg = objectMapper.readValue(record.value(), typeReference);
                spans.addAll(msg);
            } catch (IOException e) {
                log.error("Failed to process message [event] failed", e);
            }
        }

        processor.process(getTopic(), spans);
    }
}