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

package org.bithon.server.storage.druid;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.jdbc.utils.SqlDialectManager;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricStorageConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("druid")
public class MetricStorage extends MetricJdbcStorage {

    private final ObjectMapper objectMapper;
    private DruidConfig config;

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                         @JacksonInject(useInput = OptBoolean.FALSE) DruidJooqContextHolder dslContextHolder,
                         @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager,
                         @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialect,
                         @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig config) {
        super(dslContextHolder.getDslContext(), schemaManager, config, sqlDialect);
        this.objectMapper = objectMapper;
    }

    @Override
    protected void initialize(DataSourceSchema schema, MetricTable table) {
        new TableCreator(config, this.dslContext).createIfNotExist(table);
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        return new MetricWriter(schema.getName(), this.config, this.objectMapper);
    }

    static class MetricWriter implements IMetricWriter {

        private final KafkaProducer<String, String> kafkaProducer;
        private final ObjectMapper objectMapper;
        private final DruidConfig druidConfig;
        private final String topic;

        private final String name;

        MetricWriter(String name, DruidConfig druidConfig, ObjectMapper objectMapper) {
            this.name = name;

            // Since we're going to change this property object, duplicate it first
            Map<String, Object> kafkaProperties = new HashMap<>(druidConfig.getKafka().getMetrics());

            this.topic = (String) kafkaProperties.remove("topic");
            kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            this.kafkaProducer = new KafkaProducer<>(kafkaProperties);
            this.objectMapper = objectMapper;
            this.druidConfig = druidConfig;
        }

        @Override
        public void write(List<IInputRow> inputRowList) {
            String key = null;

            StringBuilder messageBuilder = new StringBuilder(512);
            for (IInputRow row : inputRowList) {
                try {
                    String metricBody = objectMapper.writeValueAsString(row);

                    long timestamp = (long) row.deleteColumn("timestamp");
                    String appName = (String) row.deleteColumn("appName");
                    String instanceName = (String) row.deleteColumn("instanceName");

                    if (key == null) {
                        key = name + "?appName=" + appName;
                    }

                    messageBuilder.append('{');
                    messageBuilder.append(StringUtils.format("\"timestamp\": %d,", timestamp));
                    messageBuilder.append(StringUtils.format("\"type\": \"%s\",", name));
                    messageBuilder.append(StringUtils.format("\"appName\": \"%s\",", appName));
                    messageBuilder.append(StringUtils.format("\"instanceName\": \"%s\", ", instanceName));
                    messageBuilder.append("\"metrics\":");
                    messageBuilder.append(metricBody);
                    messageBuilder.append("}\n");
                } catch (JsonProcessingException ignored) {
                }
            }

            // construct the producer out of a writer to be shared
            kafkaProducer.send(new ProducerRecord<>(this.topic, key, messageBuilder.toString()));
        }

        @Override
        public void close() {
            kafkaProducer.close();
        }
    }
}
