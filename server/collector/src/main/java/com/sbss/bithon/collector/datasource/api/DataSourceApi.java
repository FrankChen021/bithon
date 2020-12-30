package com.sbss.bithon.collector.datasource.api;

import com.sbss.bithon.collector.common.utils.datetime.TimeSpan;
import com.sbss.bithon.collector.datasource.DataSourceSchema;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

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
    private final IMetricStorage metricStorage;
    private final DataSourceSchemaManager schemaManager;

    public DataSourceApi(IMetricStorage metricStorage,
                         DataSourceSchemaManager schemaManager) {
        this.metricStorage = metricStorage;
        this.schemaManager = schemaManager;
    }

    @PostMapping("/api/datasource/metrics")
    public List<Map<String, Object>> getMetrics(@Valid @RequestBody GetMetricsRequest request) {

        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        return this.metricStorage.createMetricReader(schema).getMetricValueList(
            TimeSpan.fromISO8601(request.getStartTimeISO8601()),
            TimeSpan.fromISO8601(request.getEndTimeISO8601()),
            schema,
            request.getDimensions(),
            request.getMetrics()
        );
    }

    @PostMapping("/api/datasource/sql")
    public List<Map<String, Object>> getMetricsBySql(@Valid @RequestBody GetMetricsBySqlRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        return this.metricStorage.createMetricReader(schema).getMetricValueList(request.getSql());
    }

    @PostMapping("/api/datasource/schemas")
    public Map<String, DataSourceSchema> getSchemas() {
        return schemaManager.getDataSources();
    }

    @PostMapping("/api/datasource/schema/{name}")
    public DataSourceSchema getSchemaByName(@PathVariable("name") String schemaName) {
        return schemaManager.getDataSourceSchema(schemaName);
    }

    @Getter
    @AllArgsConstructor
    static class DataSourceName {
        private final String name;
        private final String displayText;
    }

    @PostMapping("/api/datasource/name")
    public Collection<DataSourceName> getSchemaNames() {
        return schemaManager.getDataSources()
            .values()
            .stream()
            .map(schema -> new DataSourceName(schema.getName(), schema.getDisplayText()))
            .collect(Collectors.toList());
    }
}
