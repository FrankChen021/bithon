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

package org.bithon.server.web.service.datasource.api.impl;

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.TTLConfig;
import org.bithon.server.storage.datasource.DataSourceExistException;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.ListQuery;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.storage.metrics.Query;
import org.bithon.server.storage.metrics.TimeseriesQuery;
import org.bithon.server.web.service.datasource.api.DataSourceService;
import org.bithon.server.web.service.datasource.api.DisplayableText;
import org.bithon.server.web.service.datasource.api.GeneralQueryRequest;
import org.bithon.server.web.service.datasource.api.GetDimensionRequest;
import org.bithon.server.web.service.datasource.api.GroupByQueryRequest;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.ListQueryRequest;
import org.bithon.server.web.service.datasource.api.ListQueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryRequest;
import org.bithon.server.web.service.datasource.api.UpdateTTLRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
public class DataSourceApi implements IDataSourceApi {
    private final MetricStorageConfig storageConfig;
    private final IMetricStorage metricStorage;
    private final DataSourceSchemaManager schemaManager;
    private final DataSourceService dataSourceService;

    public DataSourceApi(MetricStorageConfig storageConfig,
                         IMetricStorage metricStorage,
                         DataSourceSchemaManager schemaManager,
                         DataSourceService dataSourceService) {
        this.storageConfig = storageConfig;
        this.metricStorage = metricStorage;
        this.schemaManager = schemaManager;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public DataSourceService.TimeSeriesQueryResult timeseries(TimeSeriesQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        TimeSpan start = TimeSpan.fromISO8601(request.getInterval().getStartISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getInterval().getEndISO8601());

        return dataSourceService.timeseriesQuery(TimeseriesQuery.builder()
                                                                .dataSource(schema)
                                                                .aggregators(CollectionUtils.emptyOrOriginal(request.getAggregators()))
                                                                .metrics(CollectionUtils.emptyOrOriginal(request.getMetrics()))
                                                                .filters(CollectionUtils.emptyOrOriginal(request.getFilters()))
                                                                .interval(Interval.of(start, end, request.getInterval().getStep()))
                                                                .groupBy(CollectionUtils.emptyOrOriginal(request.getGroupBy()))
                                                                .build());
    }

    @Override
    public List<Map<String, Object>> timeseriesV3(GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = this.dataSourceService.convertToQuery(schema, request, true);
        return this.metricStorage.createMetricReader(schema).timeseries(query);
    }

    @Override
    public List<Map<String, Object>> groupBy(GroupByQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        List<Object> metrics = request.getMetrics().stream().map(schema::getMetricSpecByName).collect(Collectors.toList());
        metrics.addAll(request.getAggregators());
        metrics.addAll(request.getGroupBy());
        
        return this.metricStorage.createMetricReader(schema).groupBy(Query.builder()
                                                                          .dataSource(schema)
                                                                          .fields(metrics)
                                                                          .filters(request.getFilters())
                                                                          .interval(Interval.of(start, end))
                                                                          .groupBy(request.getGroupBy())
                                                                          .orderBy(request.getOrderBy()).build());
    }

    @Override
    public ListQueryResponse list(ListQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        ListQuery query = new ListQuery(schema,
                                        request.getColumns(),
                                        request.getFilters(),
                                        Interval.of(TimeSpan.fromISO8601(request.getStartTimeISO8601()), TimeSpan.fromISO8601(request.getEndTimeISO8601())),
                                        request.getOrderBy(),
                                        request.getPageNumber(),
                                        request.getPageSize());

        // TODO: refactor the storage reader so that the data source is working for all storages
        IMetricReader reader = this.metricStorage.createMetricReader(schema);
        return new ListQueryResponse(reader.listSize(query), reader.list(query));
    }

    @Override
    public List<Map<String, Object>> groupBy(GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = this.dataSourceService.convertToQuery(schema, request, false);
        return this.metricStorage.createMetricReader(schema).groupBy(query);
    }

    @Override
    public Map<String, DataSourceSchema> getSchemas() {
        return schemaManager.getDataSources();
    }

    @Override
    public DataSourceSchema getSchemaByName(String schemaName) {
        return schemaManager.getDataSourceSchema(schemaName);
    }


    @Override
    public void createSchema(@RequestBody DataSourceSchema schema) {
        if (schemaManager.containsSchema(schema.getName())) {
            throw new DataSourceExistException(schema.getName());
        }

        // TODO:
        // use writer to initialize the underlying storage
        // in the future, the initialization should be extracted out of the writer
        try (IMetricWriter writer = this.metricStorage.createMetricWriter(schema)) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schemaManager.addDataSourceSchema(schema);

        if (schema.getInputSourceSpec() != null) {
            schema.getInputSourceSpec().start(schema);
        }
    }

    @Override
    public void updateSchema(@RequestBody DataSourceSchema newSchema) {
        DataSourceSchema oldSchema = schemaManager.getDataSourceSchema(newSchema.getName());

        schemaManager.updateDataSourceSchema(newSchema);

        // TODO: if dimensions/metrics change, need to update the underlying storage schema

        //
        if (oldSchema.getInputSourceSpec() != null) {
            oldSchema.getInputSourceSpec().stop();
        }

        if (newSchema.getInputSourceSpec() != null) {
            newSchema.getInputSourceSpec().start(newSchema);
        }
    }

    @Override
    public Collection<DisplayableText> getSchemaNames() {
        return schemaManager.getDataSources()
                            .values()
                            .stream()
                            .map(schema -> new DisplayableText(schema.getName(), schema.getDisplayText()))
                            .collect(Collectors.toList());
    }

    @Override
    public Collection<Map<String, String>> getDimensions(GetDimensionRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        IDimensionSpec dimensionSpec;
        if ("name".equals(request.getType())) {
            dimensionSpec = schema.getDimensionSpecByName(request.getName());
        } else if ("alias".equals(request.getType())) {
            dimensionSpec = schema.getDimensionSpecByAlias(request.getName());
        } else {
            throw new Preconditions.InvalidValueException("'type' should be one of (name, alias)");
        }
        Preconditions.checkNotNull(dimensionSpec, "dimension [%s] not defined.", request.getName());

        return this.metricStorage.createMetricReader(schema)
                                 .getDimensionValueList(TimeSpan.fromISO8601(request.getStartTimeISO8601()),
                                                        TimeSpan.fromISO8601(request.getEndTimeISO8601()),
                                                        schema,
                                                        request.getFilters(),
                                                        dimensionSpec.getName());
    }

    @Override
    public void updateSpecifiedDataSourceTTL(@RequestBody UpdateTTLRequest request) {
        TTLConfig ttlConfig = this.storageConfig.getTtl();
        ttlConfig.setTtl(request.getTtl());
    }
}
