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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bithon.server.commons.logging.RateLimitLogger;
import org.bithon.server.pipeline.common.kafka.AbstractKafkaConsumer;
import org.bithon.server.pipeline.tracing.ITraceProcessor;
import org.bithon.server.storage.tracing.TraceSpan;
import org.slf4j.event.Level;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public class KafkaTraceReceiver extends AbstractKafkaConsumer implements ITraceReceiver {
    private static final RateLimitLogger LOG = new RateLimitLogger(log).config(Level.ERROR, 1);

    private final Map<String, Object> props;
    private final JavaType spanJavaType;
    private ITraceProcessor processor;

    public KafkaTraceReceiver(@JsonProperty("props") Map<String, Object> props,
                              @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                              @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        super(objectMapper, applicationContext);
        this.props = props;

        this.spanJavaType = objectMapper.getTypeFactory().constructType(TraceSpan.class);
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
                JsonToken token = jsonParser.nextToken();
                if (token == JsonToken.START_ARRAY) {
                    // JSONArray format
                    while (jsonParser.nextToken() == JsonToken.START_OBJECT) {
                        TraceSpan span = objectMapper.readValue(jsonParser, this.spanJavaType);
                        spans.add(span);
                    }
                } else if (token == JsonToken.START_OBJECT) {
                    // JSONEachRow format
                    Iterator<TraceSpan> iterator = objectMapper.readValues(jsonParser, this.spanJavaType);
                    iterator.forEachRemaining(spans::add);
                }
            } catch (IOException e) {
                LOG.error("Failed to process tracing message", e);
            }
        }

        processor.process(getTopic(), spans);
    }
}
