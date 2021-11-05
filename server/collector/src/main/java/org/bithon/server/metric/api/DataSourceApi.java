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

package org.bithon.server.metric.api;

import org.bithon.server.common.pojo.DisplayableText;
import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.storage.GroupByQuery;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.Interval;
import org.bithon.server.metric.storage.MetricStorageConfig;
import org.bithon.server.metric.storage.TimeseriesQuery;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:19 下午
 */
@CrossOrigin
@RestController
public class DataSourceApi {
    private final MetricStorageConfig storageConfig;
    private final IMetricStorage metricStorage;
    private final DataSourceSchemaManager schemaManager;

    public DataSourceApi(MetricStorageConfig storageConfig,
                         IMetricStorage metricStorage,
                         DataSourceSchemaManager schemaManager) {
        this.storageConfig = storageConfig;
        this.metricStorage = metricStorage;
        this.schemaManager = schemaManager;
    }

    @PostMapping("/api/datasource/metrics")
    public List<Map<String, Object>> timeseries(@Valid @RequestBody GetMetricsRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        return this.metricStorage.createMetricReader(schema)
                                 .timeseries(new TimeseriesQuery(schema,
                                                                 request.getMetrics(),
                                                                 request.getDimensions().values(),
                                                                 Interval.of(start, end),
                                                                 request.getGroups()));
    }

    @PostMapping("/api/datasource/groupBy")
    public List<Map<String, Object>> groupBy(@Valid @RequestBody GroupByQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        return this.metricStorage.createMetricReader(schema).groupBy(new GroupByQuery(schema,
                                                                                      request.getMetrics(),
                                                                                      request.getAggregators(),
                                                                                      request.getFilters().values(),
                                                                                      Interval.of(start, end),
                                                                                      request.getGroupBy()));
    }

    @PostMapping("/api/datasource/schemas")
    public Map<String, DataSourceSchema> getSchemas() {
        return schemaManager.getDataSources();
    }

    @PostMapping("/api/datasource/schema/{name}")
    public DataSourceSchema getSchemaByName(@PathVariable("name") String schemaName) {
        return schemaManager.getDataSourceSchema(schemaName);
    }

    @PostMapping("/api/datasource/name")
    public Collection<DisplayableText> getSchemaNames() {
        return schemaManager.getDataSources()
                            .values()
                            .stream()
                            .map(schema -> new DisplayableText(schema.getName(), schema.getDisplayText()))
                            .collect(Collectors.toList());
    }

    @PostMapping("/api/datasource/dimensions")
    public Collection<Map<String, String>> getDimensions(@Valid @RequestBody GetDimensionRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        return this.metricStorage.createMetricReader(schema).getDimensionValueList(
            TimeSpan.fromISO8601(request.getStartTimeISO8601()),
            TimeSpan.fromISO8601(request.getEndTimeISO8601()),
            schema,
            request.getConditions(),
            request.getDimension()
        );
    }

    @PostMapping("api/datasource/ttl/update")
    public void updateSpecifiedDataSourceTTL(@RequestBody UpdateTTLRequest request) {
        this.storageConfig.setTtl(request.getTtl());
    }
}
