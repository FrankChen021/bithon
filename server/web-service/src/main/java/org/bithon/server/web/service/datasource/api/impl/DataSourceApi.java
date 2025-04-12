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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.pipeline.metrics.input.IMetricInputSource;
import org.bithon.server.pipeline.tracing.sampler.ITraceSampler;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaException;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.column.ExpressionColumn;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.store.IDataStoreSpec;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.DataSourceService;
import org.bithon.server.web.service.datasource.api.DisplayableText;
import org.bithon.server.web.service.datasource.api.GetDimensionRequest;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesQueryResult;
import org.bithon.server.web.service.datasource.api.UpdateTTLRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 8:19 下午
 */
@Slf4j
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class DataSourceApi implements IDataSourceApi {
    private final MetricStorageConfig storageConfig;
    private final IMetricStorage metricStorage;
    private final SchemaManager schemaManager;
    private final DataSourceService dataSourceService;
    private final Executor asyncExecutor;
    private final DiscoveredServiceInvoker discoveredServiceInvoker;

    public DataSourceApi(MetricStorageConfig storageConfig,
                         IMetricStorage metricStorage,
                         SchemaManager schemaManager,
                         DataSourceService dataSourceService,
                         DiscoveredServiceInvoker discoveredServiceInvoker) {
        this.storageConfig = storageConfig;
        this.metricStorage = metricStorage;
        this.schemaManager = schemaManager;
        this.dataSourceService = dataSourceService;
        this.asyncExecutor = new ThreadPoolExecutor(0,
                                                    32,
                                                    180L,
                                                    TimeUnit.SECONDS,
                                                    new SynchronousQueue<>(),
                                                    NamedThreadFactory.nonDaemonThreadFactory("datasource-async"));
        this.discoveredServiceInvoker = discoveredServiceInvoker;
    }

    @Override
    public QueryResponse timeseriesV3(@Validated @RequestBody QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, false, true);
        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery(query);
        return QueryResponse.builder()
                            .meta(query.getSelectors().stream().map((selector) -> new QueryResponse.QueryResponseColumn(selector.getOutputName(),
                                                                                                                        selector.getDataType().name())).toList())
                            .data(result.getMetrics())
                            .startTimestamp(result.getStartTimestamp())
                            .endTimestamp(result.getEndTimestamp())
                            .interval(result.getInterval())
                            .build();
    }

    @Override
    public QueryResponse timeseriesV4(@Validated @RequestBody QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, true, true);
        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery(query);
        return QueryResponse.builder()
                            .meta(query.getSelectors().stream().map((selector) -> new QueryResponse.QueryResponseColumn(selector.getOutputName(), selector.getDataType().name())).toList())
                            .data(result.getMetrics())
                            .startTimestamp(result.getStartTimestamp())
                            .endTimestamp(result.getEndTimestamp())
                            .interval(result.getInterval())
                            .build();
    }

    @Override
    public QueryResponse timeseriesV5(@Validated @RequestBody QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, true, true);

        List<QueryResponse.QueryResponseColumn> columns = new ArrayList<>();
        columns.add(new QueryResponse.QueryResponseColumn("_timestamp", IDataType.LONG.name()));
        columns.addAll(query.getGroupBy()
                            .stream()
                            .map((s) -> {
                                IColumn column = schema.getColumnByName(s);
                                Preconditions.checkNotNull(column, "Field [%s] given in the GROUP-BY expression does not exist in the schema.", s);
                                return new QueryResponse.QueryResponseColumn(column.getName(), column.getDataType().name());
                            }).toList());
        columns.addAll(query.getSelectors()
                            .stream()
                            .map((selector) -> new QueryResponse.QueryResponseColumn(selector.getOutputName(), selector.getDataType().name()))
                            .toList());

        TimeSeriesQueryResult result = this.dataSourceService.timeseriesQuery2(query);
        return QueryResponse.builder()
                            .meta(columns)
                            .data(result.getMetrics())
                            .startTimestamp(result.getStartTimestamp())
                            .endTimestamp(result.getEndTimestamp())
                            .interval(result.getInterval())
                            .build();
    }

    @Override
    public QueryResponse list(QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toSelectQuery(schema, request);

        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {

            CompletableFuture<Integer> total = CompletableFuture.supplyAsync(() -> {
                // Only query the total number of records for the first page
                // This also has a restriction
                // that the page number is not a query parameter on web page URL
                return request.getLimit().getOffset() == 0 ? reader.count(query) : 0;
            }, asyncExecutor);

            CompletableFuture<List<?>> list = CompletableFuture.supplyAsync(() -> {
                // The query is executed in an async task, and the filter AST might be optimized in further processing
                // To make sure the optimization is thread safe, we create a new AST
                IExpression filter = QueryFilter.build(schema, request.getFilterExpression());
                return reader.select(query.with(filter));
            }, asyncExecutor);

            try {
                return QueryResponse.builder()
                                    .meta(query.getSelectors()
                                               .stream()
                                               .map((selector) -> new QueryResponse.QueryResponseColumn(selector.getOutputName(), selector.getDataType().name()))
                                               .toList())
                                    .total(total.get())
                                    .limit(query.getLimit())
                                    .data(list.get())
                                    .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                    .startTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                    .build();
            } catch (ExecutionException e) {
                String message = "Failed to execute the query: " + e.getCause().getMessage();
                log.error(message, e.getCause());
                throw new HttpMappableException(e.getCause(),
                                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                                message);
            } catch (InterruptedException e) {
                throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
            }
        }
    }

    @Override
    public QueryResponse groupBy(QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, false, false);
        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            return QueryResponse.builder()
                                .meta(query.getSelectors().stream().map((selector) -> new QueryResponse.QueryResponseColumn(selector.getOutputName(), selector.getDataType().name())).toList())
                                .startTimestamp(query.getInterval().getStartTime().getMilliseconds())
                                .endTimestamp(query.getInterval().getEndTime().getMilliseconds())
                                .data(reader.groupBy(query))
                                .build();
        }
    }

    @Override
    public QueryResponse groupByV3(QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, true, false);

        List<QueryResponse.QueryResponseColumn> groupByColumns = query.getGroupBy()
                                                                      .stream()
                                                                      .map((groupBy) -> {
                                                                          IColumn column = schema.getColumnByName(groupBy);
                                                                          Preconditions.checkNotNull(column, "Field [%s] given in the GROUP-BY expression does not exist in the schema.", groupBy);
                                                                          return new QueryResponse.QueryResponseColumn(column.getName(), column.getDataType().name());
                                                                      })
                                                                      .toList();

        List<QueryResponse.QueryResponseColumn> selectColumns = query.getSelectors()
                                                                     .stream()
                                                                     .map((selector) -> new QueryResponse.QueryResponseColumn(selector.getOutputName(), selector.getDataType().name()))
                                                                     .toList();
        List<QueryResponse.QueryResponseColumn> returnColumns = new ArrayList<>();
        returnColumns.addAll(groupByColumns);
        returnColumns.addAll(selectColumns);

        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            return QueryResponse.builder()
                                .meta(returnColumns)
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
        ISchema schema = schemaManager.getSchema(schemaName, false);

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
    public IMetricInputSource.SamplingResult testSchema(ISchema schema) {
        if (schema.getInputSourceSpec() == null || schema.getInputSourceSpec().isNull()) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Input source is not specified in the schema");
        }

        JsonNode inputSourceType = schema.getInputSourceSpec().get("type");
        if (inputSourceType == null || inputSourceType.isNull()) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Input source type is not specified in the schema");
        }

        if (!"span".equals(inputSourceType.asText())) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Only input source [span] is supported for now");
        }

        ITraceSampler pipelineApi = this.discoveredServiceInvoker.createUnicastApi(ITraceSampler.class);
        return pipelineApi.sample(schema);
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
        Preconditions.checkIfTrue(!(column instanceof IAggregatableColumn), "Can't get values on type of aggregatable column [%s]", column.getName());
        Preconditions.checkIfTrue(!(column instanceof ExpressionColumn), "Can't get values on type of expression column [%s]", column.getName());

        try (IDataSourceReader reader = schema.getDataStoreSpec().createReader()) {
            Query query = Query.builder()
                               .interval(Interval.of(request.getStartTimeISO8601(), request.getEndTimeISO8601()))
                               .schema(schema)
                               .selectors(Collections.singletonList(column.toSelector()))
                               .filter(QueryFilter.build(schema, request.getFilterExpression()))
                               .build();

            return reader.distinct(query)
                         .stream()
                         .map((val) -> {
                             Map<String, String> map = new HashMap<>();
                             map.put("value", val);
                             return map;
                         }).collect(Collectors.toList());
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
}
