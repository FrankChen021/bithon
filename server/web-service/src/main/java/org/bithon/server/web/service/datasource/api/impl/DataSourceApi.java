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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.SchemaException;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.store.IDataStoreSpec;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.pipeline.tracing.sampler.ITraceSampler;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.datasource.SchemaManager;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.ThreadLocalRandom;
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
    private final SchemaManager schemaManager;
    private final DataSourceService dataSourceService;
    private final Executor asyncExecutor;
    private final DiscoveredServiceInvoker discoveredServiceInvoker;
    private final ObjectMapper objectMapper;

    public DataSourceApi(MetricStorageConfig storageConfig,
                         SchemaManager schemaManager,
                         DataSourceService dataSourceService,
                         DiscoveredServiceInvoker discoveredServiceInvoker,
                         ObjectMapper objectMapper) {
        this.storageConfig = storageConfig;
        this.schemaManager = schemaManager;
        this.dataSourceService = dataSourceService;
        this.asyncExecutor = new ThreadPoolExecutor(0,
                                                    32,
                                                    180L,
                                                    TimeUnit.SECONDS,
                                                    new SynchronousQueue<>(),
                                                    NamedThreadFactory.nonDaemonThreadFactory("datasource-async"));
        this.discoveredServiceInvoker = discoveredServiceInvoker;
        this.objectMapper = objectMapper;
    }

    @Override
    public QueryResponse timeseriesV4(@Validated @RequestBody QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, true);
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
    public ColumnarTable timeseriesV5(@Validated @RequestBody QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, true);

        return this.dataSourceService.timeseriesQuery2(query);
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
    public QueryResponse groupByV3(QueryRequest request) throws IOException {
        ISchema schema = schemaManager.getSchema(request.getDataSource());

        Query query = QueryConverter.toQuery(schema, request, false);

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
    public ResponseEntity<StreamingResponseBody> testSchema(ISchema schema) {
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

        // Instead of using Feign, make a direct HTTP request to proxy the streaming response
        return proxyTraceSamplerRequest(schema);
    }

    private ResponseEntity<StreamingResponseBody> proxyTraceSamplerRequest(ISchema schema) {
        try {
            List<DiscoveredServiceInstance> instances = discoveredServiceInvoker.getInstanceList(ITraceSampler.class);
            if (instances.isEmpty()) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(), "No pipeline service instances available");
            }
            DiscoveredServiceInstance instance = instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
            String targetUrlString = instance.getURL() + "/api/pipeline/tracing/sample";

            final String body = objectMapper.writeValueAsString(schema);

            // Configure the HttpClient.
            final CloseableHttpClient httpClient = HttpClients.custom()
                                                              .setDefaultRequestConfig(RequestConfig.custom()
                                                                                                    .setConnectTimeout(5000)
                                                                                                    .setSocketTimeout(15000)
                                                                                                    .setConnectionRequestTimeout(5000)
                                                                                                    .build())
                                                              .build();

            HttpPost httpPost = new HttpPost(targetUrlString);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Connection", "keep-alive");
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));

            CloseableHttpResponse httpResponse;
            try {
                httpResponse = httpClient.execute(httpPost);
            } catch (Exception e) {
                try {
                    httpClient.close();
                } catch (Throwable ignored) {
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                     .body(outputStream -> {
                                         String errorMessage = "Failed to proxy trace sampler request: " + e.getMessage();
                                         outputStream.write(errorMessage.getBytes());
                                         outputStream.flush();
                                     });
            }

            HttpHeaders headers = new HttpHeaders();
            Header header = httpResponse.getLastHeader("Content-Type");
            if (header != null) {
                headers.setContentType(MediaType.parseMediaType(header.getValue()));
            }

            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.OK.value()) {
                byte[] errorBody = EntityUtils.toByteArray(httpResponse.getEntity());
                try {
                    httpResponse.close();
                } catch (Throwable ignored) {
                }
                try {
                    httpClient.close();
                } catch (Throwable ignored) {
                }
                return ResponseEntity.status(statusCode)
                                     .headers(headers)
                                     .body(outputStream -> {
                                         outputStream.write(errorBody);
                                         outputStream.flush();
                                     });
            }

            StreamingResponseBody streamingBody = outputStreamToClient -> {
                try (CloseableHttpResponse response = httpResponse) {
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity == null) {
                        return;
                    }

                    // Get the content stream from the response entity
                    try (InputStream inputStreamFromServer = responseEntity.getContent()) {
                        int bytesRead;
                        byte[] buffer = new byte[4096];
                        while ((bytesRead = inputStreamFromServer.read(buffer)) != -1) {
                            if (bytesRead > 0) {
                                outputStreamToClient.write(buffer, 0, bytesRead);
                                outputStreamToClient.flush(); // Important: flush to the client immediately
                            }
                        }
                    }
                } finally {
                    httpClient.close();
                }
            };

            return ResponseEntity.ok()
                                 .headers(headers)
                                 .body(streamingBody);

        } catch (
            Exception e) {
            log.error("Failed to proxy request using Apache HttpClient: {}", e.getMessage(), e);
            throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to proxy trace sampler request (Apache HttpClient): " + e.getMessage());
        }
    }

    @Override
    public void createSchema(@RequestBody ISchema schema) {
        if (schemaManager.containsSchema(schema.getName())) {
            throw new SchemaException.AlreadyExists(schema.getName());
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
