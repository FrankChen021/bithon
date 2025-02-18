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

package org.bithon.server.pipeline.common.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bithon.component.commons.utils.NumberUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
@Slf4j
public abstract class AbstractKafkaConsumer implements IKafkaConsumer, BatchMessageListener<String, byte[]> {
    private final ApplicationContext applicationContext;

    private ConcurrentMessageListenerContainer<String, String> consumerContainer;

    @Getter
    private String topic;

    public AbstractKafkaConsumer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public IKafkaConsumer start(final Map<String, Object> props) {
        int pollTimeout = NumberUtils.getInteger(props.remove("pollTimeout"), 1000);
        Preconditions.checkIfTrue(pollTimeout >= 100, "'pollTimeout' must be >= 100, given value is %d", pollTimeout);

        int ackTime = NumberUtils.getInteger(props.remove("ackTime"), 5000);
        Preconditions.checkIfTrue(ackTime >= 100 && ackTime <= 60_000, "'ackTime' must be >= 100 && <= 60_000, given value is %d", ackTime);

        int concurrency = NumberUtils.getInteger(props.remove("concurrency"), 1);
        Preconditions.checkIfTrue(concurrency > 0 && concurrency <= 64, "'concurrency' must be > 0 and <= 64, given values is: %d", concurrency);

        topic = (String) props.remove("topic");
        Preconditions.checkNotNull(topic, "topic for [%s] is not configured.", this.getClass().getSimpleName());

        Map<String, Object> consumerProperties = new HashMap<>(props);
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        ContainerProperties containerProperties = new ContainerProperties(topic);
        containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
        containerProperties.setAckTime(ackTime);
        containerProperties.setPollTimeout(pollTimeout);
        containerProperties.setGroupId((String) props.getOrDefault(ConsumerConfig.GROUP_ID_CONFIG, "bithon-" + topic));
        containerProperties.setClientId((String) props.getOrDefault(ConsumerConfig.CLIENT_ID_CONFIG, "bithon-" + topic));
        consumerContainer = new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties);

        // the Spring Kafka uses the bean name as prefix of thread name
        // Since tracing records thread name automatically to span logs, we explicitly set the bean name to improve the readability of span logs
        consumerContainer.setBeanName(this.getClass().getSimpleName());

        consumerContainer.setupMessageListener(this);
        consumerContainer.setConcurrency(concurrency);
        consumerContainer.setApplicationEventPublisher(applicationContext);
        consumerContainer.setApplicationContext(applicationContext);
        /*
        consumerContainer.setCommonErrorHandler(new ContainerAwareBatchErrorHandler() {
            @Override
            public void handle(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer) {
                this.handle(thrownException, data, consumer, consumerContainer);
            }

            @Override
            public void handle(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer, MessageListenerContainer container) {
                // Ignore exception when committing offset
                if (thrownException instanceof TimeoutException) {
                    if (thrownException.getMessage().contains("committing")) {
                        log.info("Timeout to commit offset ignored.");
                        return;
                    }
                }

                // Fallback to default handler to handle
                //new RecoveringBatchErrorHandler().handle(thrownException, data, consumer, container);
            }
        });*/
        consumerContainer.start();

        log.info("Starting Kafka consumer for topic [{}]", topic);
        return this;
    }

    @Override
    public void stop() {
        log.info("Stopping Kafka consumer for {}", this.topic);
        if (consumerContainer != null) {
            consumerContainer.stop(true);
        }
    }

    @Override
    public boolean isRunning() {
        return consumerContainer.isRunning();
    }

    @Override
    public abstract void onMessage(List<ConsumerRecord<String, byte[]>> records);
}
