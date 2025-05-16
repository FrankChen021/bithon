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

package org.bithon.server.alerting.notification.channel.kafka;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.message.NotificationMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 */
@Slf4j
public class KafkaNotificationChannel implements INotificationChannel {

    @JsonIgnore
    private volatile KafkaProducer<String, String> alertProducer;

    @JsonIgnore
    private final ObjectMapper om;

    @Data
    public static class Props {
        @NotEmpty
        private String topic;

        @NotEmpty
        private String bootstrapServers;

        private Map<String, String> producerProps;
    }

    private final Props props;

    @JsonCreator
    public KafkaNotificationChannel(@JsonProperty("props") Props props,
                                    @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper om) {
        this.props = Preconditions.checkNotNull(props, "props property can't be null");
        Preconditions.checkIfTrue(StringUtils.hasText(this.props.topic), "topic property can not be empty");
        Preconditions.checkIfTrue(StringUtils.hasText(this.props.bootstrapServers), "bootstrapServers property can not be empty");

        this.om = om;
    }

    @Override
    public void send(NotificationMessage message) throws IOException {
        if (alertProducer == null) {
            synchronized (this) {
                if (alertProducer == null) {
                    alertProducer = createProducer(this.props.getBootstrapServers(), this.props.getProducerProps() == null ? Collections.emptyMap() : this.props.getProducerProps());
                }
            }
        }

        String content = om.writeValueAsString(message);
        log.info("Sending alert:{}", content);
        alertProducer.send(new ProducerRecord<>(this.props.getTopic(), content));
    }

    @Override
    public void test(NotificationMessage message, Duration timeout) throws Exception {
        String content = om.writeValueAsString(message);

        Map<String, String> props = new HashMap<>(this.props.getProducerProps() == null ? Collections.emptyMap() : this.props.getProducerProps());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, String.valueOf(timeout.toMillis()));
        try (KafkaProducer<String, String> producer = createProducer(this.props.bootstrapServers, props)) {
            producer.send(new ProducerRecord<>(this.props.getTopic(), content)).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void close() {
        if (this.alertProducer != null) {
            this.alertProducer.close();
        }
    }

    @Override
    public String toString() {
        return "KafkaNotificationChannel{" +
               "topic='" + this.props.topic + '\'' +
               '}';
    }

    private KafkaProducer<String, String> createProducer(String bootstrapServers, Map<String, String> props) {
        Map<String, Object> producerProps = new HashMap<>(props);
        producerProps.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(producerProps);
    }
}
