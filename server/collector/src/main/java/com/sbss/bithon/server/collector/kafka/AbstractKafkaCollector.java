package com.sbss.bithon.server.collector.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerAwareMessageListener;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18
 */
public abstract class AbstractKafkaCollector<MSG> implements IKafkaCollector {
    ConcurrentMessageListenerContainer<String, String> consumerContainer;

    protected final ObjectMapper objectMapper;
    private Class<MSG> clazz;

    public AbstractKafkaCollector(Class<MSG> clazz) {
        this.clazz = clazz;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    protected abstract String getGroupId();

    protected abstract String[] getTopics();

    protected abstract void onMessage(String topic, MSG msg);

    protected void onMessage(String topic, String rawMessage) {
        try {
            onMessage(topic, objectMapper.readValue(rawMessage, clazz));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IKafkaCollector start(Map<String, Object> consumerProps) {

        ContainerProperties containerProperties = new ContainerProperties(getTopics());
        containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
        containerProperties.setAckTime(5000);
        containerProperties.setPollTimeout(1000);
        containerProperties.setGroupId(getGroupId());
        containerProperties.setClientId(getGroupId());
        consumerContainer = new ConcurrentMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(consumerProps),
                                                                     containerProperties);
        consumerContainer.setupMessageListener(new ConsumerAwareMessageListener<String, String>() {
            @Override
            public void onMessage(ConsumerRecord<String, String> consumerRecord) {
                AbstractKafkaCollector.this.onMessage(consumerRecord.topic(),
                                                      consumerRecord.value());
            }

            @Override
            public void onMessage(ConsumerRecord<String, String> consumerRecord,
                                  Consumer<?, ?> consumer) {
                AbstractKafkaCollector.this.onMessage(consumerRecord.topic(),
                                                      consumerRecord.value());
            }
        });
        consumerContainer.start();

        return this;
    }

    @Override
    public void stop() {
        if (consumerContainer != null) {
            consumerContainer.stop(true);
        }
    }

    @Override
    public boolean isRunning() {
        return consumerContainer.isRunning();
    }
}