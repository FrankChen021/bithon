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

package org.bithon.server.collector.sink.kafka;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.record.AbstractRecords;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.collector.source.brpc.BrpcTraceCollector;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@Slf4j
public class KafkaTraceSink implements ITraceMessageSink {

    private final KafkaTemplate<byte[], byte[]> producer;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final CompressionType compressionType;
    private final Header header;

    private final ThreadLocal<FixedSizeBuffer> bufferThreadLocal;
    private final BrpcTraceCollector brpcCollector;

    @JsonCreator
    public KafkaTraceSink(@JsonProperty("props") Map<String, Object> props,
                          @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                          @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        this.topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic is not configured for tracing sink");

        int maxSizePerMessage = (int) props.getOrDefault(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1024 * 1024);
        this.compressionType = CompressionType.forName((String) props.getOrDefault(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none"));
        this.bufferThreadLocal = ThreadLocal.withInitial(() -> new FixedSizeBuffer(maxSizePerMessage));

        this.header = new RecordHeader("type", "tracing".getBytes(StandardCharsets.UTF_8));

        this.producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new ByteArraySerializer(), new ByteArraySerializer()),
                                            ImmutableMap.of(ProducerConfig.CLIENT_ID_CONFIG, "trace"));
        this.objectMapper = objectMapper;
        this.brpcCollector = new BrpcTraceCollector(applicationContext);
    }

    @Override
    public void start() {
        brpcCollector.registerProcessor(this);
        brpcCollector.start();
    }

    @SneakyThrows
    @Override
    public void process(String messageType, List<TraceSpan> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        ByteBuffer messageKey = null;

        //
        // A batch message in written into a single kafka message in which each text line is a single metric message.
        //
        // Of course, we could also send messages in this batch one by one to Kafka,
        // but I don't think it has advantages over the way below.
        //
        // But since Producer/Broker has size limitation on each message, we also limit the size in case of failure on send.
        //
        FixedSizeBuffer messageBuffer = this.bufferThreadLocal.get();
        messageBuffer.reset();
        messageBuffer.writeChar('[');
        for (TraceSpan span : spans) {
            if (messageKey == null) {
                messageKey = ByteBuffer.wrap((span.getAppName() + "/" + span.getInstanceName()).getBytes(StandardCharsets.UTF_8));
            }

            byte[] serializedSpan;
            try {
                serializedSpan = objectMapper.writeValueAsBytes(span);
            } catch (JsonProcessingException ignored) {
                continue;
            }

            int currentSize = AbstractRecords.estimateSizeInBytesUpperBound(RecordBatch.CURRENT_MAGIC_VALUE,
                                                                            this.compressionType,
                                                                            messageKey,
                                                                            messageBuffer.toByteBuffer(),
                                                                            new Header[]{header});

            // plus 2 to leave 2 bytes as margin
            if (currentSize + serializedSpan.length + 2 > messageBuffer.limit()) {
                send(messageKey, messageBuffer);

                messageBuffer.reset();
                messageBuffer.writeChar('[');
            }

            messageBuffer.writeBytes(serializedSpan);
            messageBuffer.writeChar(',');
        }
        send(messageKey, messageBuffer);
    }

    private void send(ByteBuffer messageKey, FixedSizeBuffer messageBuffer) {
        if (messageBuffer.size() <= 1) {
            return;
        }

        // Remove last separator
        messageBuffer.deleteFromEnd(1);
        messageBuffer.writeChar(']');

        ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(topic, messageKey.array(), messageBuffer.toBytes());
        record.headers().add(header);
        try {
            producer.send(record);
        } catch (Exception e) {
            log.error("Error to send trace from {}", new String(messageKey.array(), StandardCharsets.UTF_8), e);
        }
    }

    @Override
    public void close() {
        try {
            this.brpcCollector.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        producer.destroy();
        bufferThreadLocal.remove();
    }
}
