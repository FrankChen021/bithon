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

package org.bithon.agent.plugin.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author frankchen
 */
public class ManagedKafkaConsumers {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ManagedKafkaConsumers.class);

    private static final ManagedKafkaConsumers INSTANCE = new ManagedKafkaConsumers();

    private final Map<String, ManagedKafkaConsumer> consumers = new ConcurrentHashMap<>();

    public static ManagedKafkaConsumers getInstance() {
        return INSTANCE;
    }

    public void register(String clientId, KafkaConsumer<?, ?> client, ConsumerConfig config) {
        if (clientId == null) {
            return;
        }

        LOG.info("Consumer {} instantiated", clientId);
        consumers.put(clientId, new ManagedKafkaConsumer(clientId, client, config));
    }

    public void unregister(String clientId) {
        if (clientId == null) {
            return;
        }

        ManagedKafkaConsumer consumer = consumers.remove(clientId);
        if (consumer != null) {
            LOG.info("Closing consumer {}", clientId);
            consumer.close();
        }
    }
}
