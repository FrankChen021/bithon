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

package org.bithon.agent.plugin.kafka.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class ManagedKafkaProducers {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ManagedKafkaProducers.class);
    private static final ManagedKafkaProducers INSTANCE = new ManagedKafkaProducers();

    private final Map<String, ManagedKafkaProducer> producers = new ConcurrentHashMap<>();

    public static ManagedKafkaProducers getInstance() {
        return INSTANCE;
    }

    public void register(String clientId, KafkaProducer<?, ?> producer) {
        if (clientId == null) {
            return;
        }

        LOG.info("Producer {} instantiated", clientId);
        producers.put(clientId,
                      new ManagedKafkaProducer(clientId, producer));
    }

    public void unregister(String clientId) {
        if (clientId == null) {
            return;
        }

        ManagedKafkaProducer producerInfo = producers.remove(clientId);
        if (producerInfo != null) {
            LOG.info("Closing producer {}", clientId);
            producerInfo.close();
        }
    }
}
