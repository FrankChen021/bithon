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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.sink.metrics.IMetricMessageSink;
import org.bithon.server.sink.metrics.SchemaMetricMessage;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sink message to Kafka.
 * <p>
 * The instance is created by {@link org.bithon.server.collector.CollectorAutoConfiguration} and
 * {@link org.bithon.server.collector.sink.SinkConfig}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
@JsonTypeName("kafka")
public class KafkaMetricSink implements IMetricMessageSink {

    private final KafkaTemplate<String, String> producer;
    private final ObjectMapper objectMapper;
    private final String topic;

    @JsonCreator
    public KafkaMetricSink(@JsonProperty("props") Map<String, Object> props,
                           @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this.topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic is not configured for metrics sink");

        this.producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props,
                                                                              new StringSerializer(),
                                                                              new StringSerializer()),
                                            ImmutableMap.of(ProducerConfig.CLIENT_ID_CONFIG, "metrics"));

        this.objectMapper = objectMapper;
    }

    @Override
    public void process(String messageType, SchemaMetricMessage message) {
        if (CollectionUtils.isEmpty(message.getMetrics())) {
            return;
        }

        String appName = message.getMetrics().get(0).getColAsString("appName");
        String instanceName = message.getMetrics().get(0).getColAsString("instanceName");
        if (appName == null || instanceName == null) {
            return;
        }

        try {
            String messageText = this.objectMapper.writeValueAsString(message);

            String key = appName + "/" + instanceName;
            ProducerRecord<String, String> record = new ProducerRecord<>(this.topic, key, messageText);
            record.headers().add("type", messageType.getBytes(StandardCharsets.UTF_8));

            this.producer.send(record);
        } catch (JsonProcessingException ignored) {
        }
    }

    @Override
    public void close() {
        this.producer.destroy();
    }
}

