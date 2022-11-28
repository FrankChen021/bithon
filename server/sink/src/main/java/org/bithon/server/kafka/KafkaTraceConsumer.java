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

package org.bithon.server.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public class KafkaTraceConsumer extends AbstractKafkaConsumer {
    private final ITraceMessageSink traceSink;
    private final TypeReference<List<TraceSpan>> typeReference;

    public KafkaTraceConsumer(ITraceMessageSink traceSink, ApplicationContext applicationContext) {
        super(applicationContext);

        this.traceSink = traceSink;
        this.typeReference = new TypeReference<List<TraceSpan>>() {
        };
    }

    @Override
    public void stop() {
        // stop receiving
        try {
            super.stop();
        } catch (Exception ignored) {
        }

        try {
            traceSink.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onMessage(List<ConsumerRecord<String, byte[]>> records) {
        List<TraceSpan> spans = new ArrayList<>(16);

        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                List<TraceSpan> msg = objectMapper.readValue(record.value(), typeReference);
                spans.addAll(msg);
            } catch (IOException e) {
                log.error("Failed to process message [event] failed", e);
            }
        }

        traceSink.process(getTopic(), spans);
    }
}
