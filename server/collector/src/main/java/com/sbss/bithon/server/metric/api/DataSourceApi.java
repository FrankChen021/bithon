/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.metric.api;

import com.sbss.bithon.server.common.pojo.DisplayableText;
import com.sbss.bithon.server.common.utils.datetime.Period;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collection;
import java.util.HashMap;
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
    private final IMetricStorage metricStorage;
    private final DataSourceSchemaManager schemaManager;

    public DataSourceApi(IMetaStorage metaStorage,
                         IMetricStorage metricStorage,
                         DataSourceSchemaManager schemaManager) {
        this.metricStorage = metricStorage;
        this.schemaManager = schemaManager;
    }

    @PostMapping("/api/datasource/metrics")
    public List<Map<String, Object>> timeseries(@Valid @RequestBody GetMetricsRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());
        int interval = getInterval(start, end);
        return this.metricStorage.createMetricReader(schema).timeseries(
            start,
            end,
            schema,
            request.getDimensions().values(),
            request.getMetrics(),
            interval
        );
    }

    /**
     * TODO: interval should be consistent with retention rules
     */
    private int getInterval(TimeSpan start, TimeSpan end) {
        long length = end.diff(start) / 1000;
        if (length >= 7 * 24 * 3600) {
            return 15 * 60;
        }
        if (length >= 3 * 24 * 3600) {
            return 10 * 60;
        }
        if (length >= 24 * 3600) {
            return 5 * 60;
        }
        if (length >= 12 * 3600) {
            return 60;
        }
        if (length >= 6 * 3600) {
            return 30;
        }
        return 10;
    }

    @PostMapping("/api/datasource/sql")
    public List<Map<String, Object>> getMetricsBySql(@Valid @RequestBody GetMetricsBySqlRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        return this.metricStorage.createMetricReader(schema).executeSql(request.getSql());
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
    public Map<String, Long> updateSpecifiedDataSourceTTL(@RequestBody UpdateTTLRequest request) {
        Map<String, Long> result = new HashMap<>();
        schemaManager.getDataSources().forEach((name, datasource) -> {
            Period ttl = request.getTtl();
            if (!CollectionUtils.isEmpty(request.getTtls())) {
                ttl = request.getTtls().getOrDefault(name, null);
            }
            if (ttl != null) {
                datasource.setTtl(ttl);
                result.put(name, ttl.getMilliseconds());
            }
        });
        return result;
    }
}
