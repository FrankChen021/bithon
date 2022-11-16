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

package org.bithon.agent.plugin.kafka.shared;

import org.apache.kafka.clients.Metadata;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.utils.AppInfoParser;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;


/**
 * @author frankchen
 */
public abstract class ManagedKafkaClient {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ManagedKafkaClient.class);

    private final String clientId;
    private Supplier<Map<MetricName, ? extends Metric>> metricsSupplier;

    private Metadata metadata;

    public ManagedKafkaClient(String clientId, Supplier<Map<MetricName, ? extends Metric>> metricsSupplier, Metadata metadata) {
        this.clientId = clientId;
        this.metricsSupplier = metricsSupplier;

        this.metadata = metadata;
        if (metadata == null) {
            LOG.error("can't get metadata object: Kafka Version:{}", AppInfoParser.getVersion());
        }
    }

    /**
     * For brokers lower than 0.10.1.0, the clusterId is not supported.
     * In such case, an empty string is returned.
     */
    public String getClusterId() {
        String clusterId = metadata.fetch().clusterResource().clusterId();
        return clusterId == null ? "" : clusterId;
    }

    public void close() {
        this.metricsSupplier = null;
        this.metadata = null;
    }

    public String getClientId() {
        return clientId;
    }

    public Map<MetricName, ? extends Metric> getKafkaMetrics() {
        return metricsSupplier.get();
    }
}
