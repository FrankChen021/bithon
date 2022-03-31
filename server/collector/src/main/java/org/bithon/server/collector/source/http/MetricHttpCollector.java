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

package org.bithon.server.collector.source.http;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.InputRow;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 2021-10-01
 */
@Slf4j
@RestController
public class MetricHttpCollector {

    private final DataSourceSchemaManager schemaManager;
    private final IMetricStorage metricStorage;

    public MetricHttpCollector(DataSourceSchemaManager schemaManager, IMetricStorage metricStorage) {
        this.schemaManager = schemaManager;
        this.metricStorage = metricStorage;
    }

    @PostMapping(path = "/api/collector/metrics")
    public void saveMetrics(@RequestBody MetricOverHttp metrics) {
        log.trace("receive metrics:{}", metrics);
        schemaManager.addDataSourceSchema(metrics.schema);
        try (IMetricWriter metricWriter = metricStorage.createMetricWriter(metrics.schema)) {

            List<InputRow> rows = metrics.metrics.stream().map(m -> {
                Map<String, Object> row = new HashMap<>();
                row.putAll(m.dimensions);
                row.putAll(m.metrics);
                row.put("timestamp", m.timestamp);
                return new InputRow(row);
            }).collect(Collectors.toList());
            metricWriter.write(rows);

        } catch (Exception e) {
            log.error("error to write metrics ", e);
        }
    }

    @Data
    static class Measurement {
        private long timestamp;
        private long interval;
        private Map<String, String> dimensions;
        private Map<String, Number> metrics;
    }

    @Data
    static class MetricOverHttp {
        private DataSourceSchema schema;
        private List<Measurement> metrics;
    }
}
