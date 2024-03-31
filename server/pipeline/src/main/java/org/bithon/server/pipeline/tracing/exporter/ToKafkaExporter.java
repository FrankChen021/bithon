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

package org.bithon.server.pipeline.tracing.exporter;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.pipeline.common.FixedSizeBuffer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
public class ToKafkaExporter implements ITraceExporter {

    private final KafkaTemplate<byte[], byte[]> producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final int maxRowsPerMessage;

    private final ThreadLocal<FixedSizeBuffer> bufferThreadLocal;

    @JsonCreator
    public ToKafkaExporter(@JsonProperty("props") Map<String, Object> props,
                           @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic is not configured for tracing sink");

        this.maxRowsPerMessage = Integer.parseInt((String) props.getOrDefault("maxRows", "5000"));
        props.remove("maxRows");

        int min = 1024 * 1024;
        int maxSizePerMessage = Math.max(min, (int) props.getOrDefault(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, min));

        // Kafka Producer requires some extra size for management fields of one batch, so we keep aside 128 bytes
        // See: DefaultRecordBatch.RECORD_BATCH_OVERHEAD
        this.bufferThreadLocal = ThreadLocal.withInitial(() -> new FixedSizeBuffer(maxSizePerMessage - 128));

        this.producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new ByteArraySerializer(), new ByteArraySerializer()),
                                            ImmutableMap.of(ProducerConfig.CLIENT_ID_CONFIG, "trace"));

        this.objectMapper = objectMapper.copy()
                                        .configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, true);
    }

    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        send(spans);
    }

    @SneakyThrows
    public void send(List<TraceSpan> spans) {
        //
        // A batch message in written into a single kafka message in which each text line is a single trace span object.
        //
        // Of course, we could also send messages in this batch one by one to Kafka,
        // but I don't think it has advantages over the way below.
        //
        // Since Producer/Broker has size limitation on each message, we also limit the size in case of failure on send.
        //
        // To reduce memory copy, objects are directly serialized into the underlying buffer.
        // There might be a waste of serialization once current span serialization causes overflow.
        FixedSizeBuffer messageBuffer = this.bufferThreadLocal.get();
        messageBuffer.clear();

        // Create a generator for this batch of message
        JsonGenerator generator = objectMapper.createGenerator(messageBuffer);

        int rows = 0;

        for (TraceSpan span : spans) {
            // Save the position in case of overflow
            messageBuffer.mark();

            boolean overflow;
            do {
                try {
                    // Write the span object into the buffer
                    objectMapper.writeValue(generator, span);

                    // Write a new line to separate each span
                    messageBuffer.writeAsciiChar('\n');

                    overflow = false;
                } catch (IOException e) {
                    if (e instanceof FixedSizeBuffer.OverflowException
                        || e.getCause() instanceof FixedSizeBuffer.OverflowException) {
                        // Reset to previous marked position
                        messageBuffer.reset();

                        overflow = true;
                    } else {
                        throw e;
                    }
                }

                if (overflow) {
                    if (messageBuffer.size() == 0) {
                        // The first span is too large
                        log.error("The size of span is too large that exceeds the buffer size [{}].", messageBuffer.capacity());
                        break;
                    }

                    // Send the message in buffer first
                    send(messageBuffer);
                    rows = 0;

                    // Create a new generator
                    // because the generator holds some internal states that can't be restored
                    generator = objectMapper.createGenerator(messageBuffer);
                }
            } while (overflow);

            // Impose a max rows limit to avoid a single message too large.
            // If the messages are from Clickhouse, the single message might be too large to be sent to Kafka.
            // This avoids the consumer side to handle a single message too large and memory exhausted.
            if (++rows >= this.maxRowsPerMessage) {
                send(messageBuffer);
                rows = 0;
            }
        }

        send(messageBuffer);
    }

    private void send(FixedSizeBuffer messageBuffer) {
        if (messageBuffer.size() <= 1) {
            return;
        }

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, null, messageBuffer.toBytes());
        try {
            producer.send(record);
        } catch (Exception e) {
            log.error("Error to send trace", e);
        }
        messageBuffer.clear();
    }

    @Override
    public void close() {
        producer.destroy();
        bufferThreadLocal.remove();
    }

    @Override
    public String toString() {
        return "export-trace-to-kafka";
    }
}
