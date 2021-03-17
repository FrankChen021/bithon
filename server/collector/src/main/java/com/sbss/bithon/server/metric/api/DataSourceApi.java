package com.sbss.bithon.server.metric.api;

import com.sbss.bithon.server.common.pojo.DisplayableText;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
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
    private final IMetricStorage metricStorage;
    private final DataSourceSchemaManager schemaManager;

    public DataSourceApi(IMetaStorage metaStorage,
                         IMetricStorage metricStorage,
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
            request.getDimensions().values(),
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
}
