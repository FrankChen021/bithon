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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaException;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
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
import org.bithon.server.web.service.datasource.api.QueryField;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.datasource.api.UpdateTTLRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
@Conditional(WebServiceModuleEnabler.class)
public class DataSourceApi implements IDataSourceApi {
    private final MetricStorageConfig storageConfig;
    private final IMetricStorage metricStorage;
    private final SchemaManager schemaManager;
    private final DataSourceService dataSourceService;

    public DataSourceApi(MetricStorageConfig storageConfig,
                         IMetricStorage metricStorage,
                         SchemaManager schemaManager,
                         DataSourceService dataSourceService) {
        this.storageConfig = storageConfig;
        this.metricStorage = metricStorage;
        this.schemaManager = schemaManager;
        this.dataSourceService = dataSourceService;
    }

    @Override
    public GeneralQueryResponse timeseriesV3(@Validated @RequestBody GeneralQueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        validateQueryRequest(schema, request);

        Query query = QueryBuilder.build(schema, request, false, true);
        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery(query);
        return GeneralQueryResponse.builder()
                                   .data(result.getMetrics())
                                   .startTimestamp(result.getStartTimestamp())
                                   .endTimestamp(result.getEndTimestamp())
                                   .interval(result.getInterval())
                                   .build();
    }

    @Override
    public GeneralQueryResponse timeseriesV4(@Validated @RequestBody GeneralQueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        validateQueryRequest(schema, request);

        Query query = QueryBuilder.build(schema, request, true, true);
        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery(query);
        return GeneralQueryResponse.builder()
                                   .data(result.getMetrics())
                                   .startTimestamp(result.getStartTimestamp())
                                   .endTimestamp(result.getEndTimestamp())
                                   .interval(result.getInterval())
                                   .build();
    }

    @Override
    public GeneralQueryResponse list(GeneralQueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        validateQueryRequest(schema, request);

        Query query = Query.builder()
                           .schema(schema)
                           .resultColumns(request.getFields()
                                                 .stream()
                                                 .map((field) -> {
                                                     IColumn spec = schema.getColumnByName(field.getField());
                                                     Preconditions.checkNotNull(spec, "Field [%s] does not exist in the schema.", field.getField());
                                                     return new ResultColumn(spec.getName(), field.getName());
                                                 }).collect(Collectors.toList()))
                           .filter(FilterExpressionToFilters.toExpression(schema, request.getFilterExpression(), request.getFilters()))
                           .interval(Interval.of(TimeSpan.fromISO8601(request.getInterval().getStartISO8601()),
                                                 TimeSpan.fromISO8601(request.getInterval().getEndISO8601())))
                           .orderBy(request.getOrderBy())
                           .limit(request.getLimit())
                           .build();

        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            return GeneralQueryResponse.builder()
                                       .total(reader.listSize(query))
                                       .data(reader.list(query))
                                       .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                       .startTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                       .build();
        }
    }

    @Override
    public GeneralQueryResponse groupBy(GeneralQueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        validateQueryRequest(schema, request);

        Query query = QueryBuilder.build(schema, request, false, false);
        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            return GeneralQueryResponse.builder()
                                       .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                       .endTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                       .data(reader.groupBy(query))
                                       .build();
        }
    }

    @Override
    public GeneralQueryResponse groupByV3(GeneralQueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());
        validateQueryRequest(schema, request);

        Query query = QueryBuilder.build(schema, request, true, false);
        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            return GeneralQueryResponse.builder()
                                       .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                       .endTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                       .data(reader.groupBy(query))
                                       .build();
        }
    }

    @Override
    public Map<String, ISchema> getSchemas() {
        return schemaManager.getSchemas();
    }

    @Override
    public ISchema getSchemaByName(String schemaName) {
        ISchema schema = schemaManager.getSchema(schemaName);

        // Mask the sensitive information
        // This is experimental
        if (schema != null && schema.getDataStoreSpec() != null) {
            IDataStoreSpec dataStoreSpec = schema.getDataStoreSpec();

            IDataStoreSpec newStore = dataStoreSpec.hideSensitiveInformation();
            if (newStore != dataStoreSpec) {
                // Use != to compare the object address
                return schema.withDataStore(newStore);
            }
        }
        return schema;
    }

    @Override
    public void createSchema(@RequestBody ISchema schema) {
        if (schemaManager.containsSchema(schema.getName())) {
            throw new SchemaException.AlreadyExists(schema.getName());
        }

        // TODO:
        // use writer to initialize the underlying storage
        // in the future, the initialization should be extracted out of the writer
        try (IMetricWriter writer = this.metricStorage.createMetricWriter(schema)) {
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        schemaManager.addSchema(schema);
    }

    @Override
    public void updateSchema(@RequestBody ISchema newSchema) {
        schemaManager.updateSchema(newSchema);
    }

    @Override
    public Collection<DisplayableText> getSchemaNames() {
        return schemaManager.getSchemas()
                            .values()
                            .stream()
                            .map(schema -> new DisplayableText(schema.getName(), schema.getDisplayText()))
                            .collect(Collectors.toList());
    }

    @Override
    public Collection<Map<String, String>> getDimensions(GetDimensionRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        IColumn column = schema.getColumnByName(request.getName());
        Preconditions.checkNotNull(column, "Field [%s] does not exist in the schema.", request.getName());

        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            return reader.distinct(TimeSpan.fromISO8601(request.getStartTimeISO8601()),
                                   TimeSpan.fromISO8601(request.getEndTimeISO8601()),
                                   schema,
                                   FilterExpressionToFilters.toExpression(schema, request.getFilterExpression(), request.getFilters()),
                                   column.getName());
        }
    }

    @Override
    public void updateSpecifiedDataSourceTTL(@RequestBody UpdateTTLRequest request) {
        ExpirationConfig expirationConfig = this.storageConfig.getTtl();
        expirationConfig.setTtl(request.getTtl());
    }

    @Override
    public List<String> getBaselineDate() {
        return this.dataSourceService.getBaseline();
    }

    @Override
    public void saveMetricBaseline(SaveMetricBaselineRequest request) {
        this.dataSourceService.addToBaseline(request.getDate(), request.getKeepDays());
    }

    /**
     * Validate the request to ensure the safety
     */
    private void validateQueryRequest(ISchema schema, GeneralQueryRequest request) {
        if (CollectionUtils.isNotEmpty(request.getGroupBy())) {
            for (String field : request.getGroupBy()) {
                Preconditions.checkNotNull(schema.getColumnByName(field),
                                           "GroupBy field [%s] does not exist in the schema.",
                                           field);
            }
        }

        OrderBy orderBy = request.getOrderBy();
        if (orderBy != null && StringUtils.hasText(orderBy.getName())) {
            IColumn column = schema.getColumnByName(orderBy.getName());
            if (column == null) {
                // ORDER BY field might be an aggregated field,
                QueryField queryField = request.getFields()
                                               .stream()
                                               .filter((filter) -> filter.getName().equals(orderBy.getName()))
                                               .findFirst()
                                               .orElse(null);

                Preconditions.checkIfTrue(queryField != null
                                              && schema.getColumnByName(queryField.getField()) != null,
                                          "OrderBy field [%s] does not exist in the schema.",
                                          orderBy.getName());

            }
        }
    }
}
