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

package org.bithon.server.pipeline.tracing.receiver;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bithon.server.pipeline.common.kafka.AbstractKafkaConsumer;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public class KafkaTraceReceiver extends AbstractKafkaConsumer implements ITraceReceiver {
    private final Map<String, Object> props;
    private ITraceProcessor processor;

    public KafkaTraceReceiver(@JsonProperty("props") Map<String, Object> props,
                              @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        super(applicationContext);
        this.props = props;
    }

    @Override
    public void start() {
        this.start(props);
    }

    @Override
    public void registerProcessor(ITraceProcessor processor) {
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

        // For quick element removing operation in later pipeline processing
        List<TraceSpan> spans = new LinkedList<>();

        for (ConsumerRecord<String, byte[]> record : records) {
            try (JsonParser jsonParser = this.objectMapper.createParser(record.value())) {
                if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                    continue;
                }

                while (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                    TraceSpan span = objectMapper.readValue(jsonParser, TraceSpan.class);
                    spans.add(span);
                }
            } catch (IOException e) {
                log.error("Failed to process message [event] failed", e);
            }
        }

        processor.process(getTopic(), spans);
    }
}
