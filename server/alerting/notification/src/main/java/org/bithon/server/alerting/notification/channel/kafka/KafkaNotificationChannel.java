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
import lombok.Getter;
import lombok.Setter;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 */
@Slf4j
public class KafkaNotificationChannel implements INotificationChannel {

    @Getter
    private final String topic;

    @Getter
    private final String bootstrapServers;

    private final Map<String, String> producerProps;

    @Getter
    @Setter
    @JsonIgnore
    private String name;

    @JsonIgnore
    private volatile KafkaProducer<String, String> alertProducer;

    @JsonIgnore
    private final ObjectMapper om;


    @JsonCreator
    public KafkaNotificationChannel(@JsonProperty("producerProps") Map<String, String> producerProps,
                                    @JsonProperty("topic") String topic,
                                    @JsonProperty("bootstrapServers") String bootstrapServers,
                                    @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper om) {
        this.producerProps = Preconditions.checkNotNull(producerProps, "producerProps can't be null");
        this.om = om;

        this.topic = Preconditions.checkArgumentNotNull(topic, "Miss [topic] property in the producerProps");
        this.bootstrapServers = Preconditions.checkArgumentNotNull(bootstrapServers, "Miss [%s] property in the producerProps");
    }

    @Override
    public void notify(NotificationMessage message) {
        if (this.alertProducer == null) {
            synchronized (this) {
                if (this.alertProducer == null) {
                    Map<String, Object> producerProps = new HashMap<>(this.producerProps);
                    producerProps.putIfAbsent(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServers);
                    producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    producerProps.putIfAbsent(ProducerConfig.LINGER_MS_CONFIG, 100);
                    this.alertProducer = new KafkaProducer<>(producerProps);
                }
            }
        }
        try {
            String content = om.writeValueAsString(message);
            log.info("Sending alert:{}", content);
            alertProducer.send(new ProducerRecord<>(this.topic, content));
        } catch (IOException e) {
            log.error(StringUtils.format("serialize message error: %s", message), e);
        }
    }

    @Override
    public String toString() {
        return "KafkaNotificationProvider{" +
            "topic='" + this.topic + '\'' +
            '}';
    }
}
