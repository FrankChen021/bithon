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

import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.datasource.DataSourceExistException;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.IColumnSpec;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.DataSourceService;
import org.bithon.server.web.service.datasource.api.DisplayableText;
import org.bithon.server.web.service.datasource.api.FilterExpressionToFilters;
import org.bithon.server.web.service.datasource.api.GeneralQueryRequest;
import org.bithon.server.web.service.datasource.api.GeneralQueryResponse;
import org.bithon.server.web.service.datasource.api.GetDimensionRequest;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.datasource.api.UpdateTTLRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:19 下午
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
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
    public GeneralQueryResponse timeseriesV3(@Validated @RequestBody GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = this.dataSourceService.convertToQuery(schema, request, false, true);
        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery(query);
        return GeneralQueryResponse.builder()
                                   .data(result.getMetrics())
                                   .startTimestamp(result.getStartTimestamp())
                                   .endTimestamp(result.getEndTimestamp())
                                   .interval(result.getInterval())
                                   .build();
    }

    @Override
    public GeneralQueryResponse timeseriesV4(@Validated @RequestBody GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = this.dataSourceService.convertToQuery(schema, request, true, true);
        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery(query);
        return GeneralQueryResponse.builder()
                                   .data(result.getMetrics())
                                   .startTimestamp(result.getStartTimestamp())
                                   .endTimestamp(result.getEndTimestamp())
                                   .interval(result.getInterval())
                                   .build();
    }

    @Override
    public GeneralQueryResponse list(GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = Query.builder()
                           .dataSource(schema)
                           .resultColumns(request.getFields()
                                                 .stream()
                                                 .map((field) -> {
                                                     IColumnSpec spec = schema.getColumnByName(field.getField());
                                                     Preconditions.checkNotNull(spec, "field [%s] does not exist in the schema.", field.getField());
                                                     return new ResultColumn(spec.getName(), field.getName());
                                                 }).collect(Collectors.toList()))
                           .filters(FilterExpressionToFilters.toFilter(schema, request.getFilterExpression(), request.getFilters()))
                           .interval(Interval.of(TimeSpan.fromISO8601(request.getInterval().getStartISO8601()),
                                                 TimeSpan.fromISO8601(request.getInterval().getEndISO8601())))
                           .orderBy(request.getOrderBy())
                           .limit(request.getLimit())
                           .build();

        IMetricReader reader = this.metricStorage.createMetricReader(schema);
        return GeneralQueryResponse.builder()
                                   .total(reader.listSize(query))
                                   .data(reader.list(query))
                                   .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                   .startTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                   .build();
    }

    @Override
    public GeneralQueryResponse groupBy(GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = this.dataSourceService.convertToQuery(schema, request, false, false);
        return GeneralQueryResponse.builder()
                                   .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                   .endTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                   .data(this.metricStorage.createMetricReader(schema).groupBy(query))
                                   .build();
    }

    @Override
    public GeneralQueryResponse groupByV3(GeneralQueryRequest request) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(request.getDataSource());

        Query query = this.dataSourceService.convertToQuery(schema, request, true, false);
        return GeneralQueryResponse.builder()
                                   .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                   .endTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                   .data(this.metricStorage.createMetricReader(schema).groupBy(query))
                                   .build();
    }

    @Override
    public Map<String, DataSourceSchema> getSchemas() {
        return schemaManager.getDataSources();
    }

    @Override
    public DataSourceSchema getSchemaByName(String schemaName) {
        DataSourceSchema schema = schemaManager.getDataSourceSchema(schemaName);

        // Mask the sensitive information
        // This is experimental
        if (schema != null && schema.getDataStoreSpec() != null) {
            IDataStoreSpec dataStoreSpec = schema.getDataStoreSpec();

            Map<String, String> properties = new TreeMap<>(dataStoreSpec.getProperties());
            properties.computeIfPresent("password", (k, old) -> "<HIDDEN>");

            return schema.withDataStore(dataStoreSpec.withProperties(properties));
        }
        return schema;
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
    }

    @Override
    public void updateSchema(@RequestBody DataSourceSchema newSchema) {
        schemaManager.updateDataSourceSchema(newSchema);
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
        ExpirationConfig expirationConfig = this.storageConfig.getTtl();
        expirationConfig.setTtl(request.getTtl());
    }
}
