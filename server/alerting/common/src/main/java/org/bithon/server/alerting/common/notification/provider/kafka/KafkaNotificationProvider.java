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

package org.bithon.server.alerting.common.notification.provider.kafka;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.notification.message.NotificationMessage;
import org.bithon.server.alerting.common.notification.provider.INotificationProvider;
import org.bithon.server.storage.alerting.IEvaluatorLogWriter;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 */
@Slf4j
public class KafkaNotificationProvider implements INotificationProvider {

    private final KafkaProducer<String, String> alertProducer;
    private final String topic;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ObjectMapper om;

    @JsonCreator
    public KafkaNotificationProvider(@JacksonInject(useInput = OptBoolean.FALSE) Environment environment,
                                     @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper om) {
        KafkaConfig config = environment.getProperty("bithon.alerting.processor.notification.properties", KafkaConfig.class);
        Map<String, Object> props = ImmutableMap.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
            ProducerConfig.LINGER_MS_CONFIG, 100,
            ProducerConfig.CLIENT_ID_CONFIG, config.getTopic()
        );
        this.alertProducer = new KafkaProducer<>(props);
        this.topic = config.getTopic();
        this.om = om;
        this.threadPoolExecutor = new ThreadPoolExecutor(1, 5,
                                                         1,
                                                         TimeUnit.MINUTES,
                                                         new LinkedBlockingQueue<>(2048),
                                                         new NamedThreadFactory("alert-kafka-notification"),
                                                         new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public void notify(IEvaluatorLogWriter evaluatorLogger, NotificationMessage message) {
        threadPoolExecutor.execute(() -> {
            try {
                String content = om.writeValueAsString(message);
                log.info("Sending alert:{}", content);
                alertProducer.send(new ProducerRecord<>(this.topic, content));
            } catch (IOException e) {
                log.error(StringUtils.format("serialize message error: %s", message), e);
            }
        });
    }

    @Data
    static class KafkaConfig {
        private String bootstrapServers;
        private String topic;
    }

    @Override
    public String toString() {
        return "KafkaNotificationProvider{" +
               "topic='" + topic + '\'' +
               '}';
    }
}
