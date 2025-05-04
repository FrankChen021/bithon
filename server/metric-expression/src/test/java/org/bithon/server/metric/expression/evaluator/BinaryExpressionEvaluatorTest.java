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

package org.bithon.server.metric.expression.evaluator;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.metric.expression.pipeline.IQueryStep;
import org.bithon.server.metric.expression.pipeline.IntermediateQueryResult;
import org.bithon.server.metric.expression.pipeline.QueryPipelineBuilder;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:27 pm
 */
@SuppressWarnings("PointlessArithmeticExpression")
public class BinaryExpressionEvaluatorTest {
    private IDataSourceApi dataSourceApi;

    @BeforeEach
    public void setUpClass() {
        dataSourceApi = Mockito.mock(IDataSourceApi.class);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(6, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 3.3");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(4.3, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 3.7)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(8.7, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 10.5)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 2.2");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(12.7, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverSizeLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5Mi");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(HumanReadableNumber.of("5Mi").longValue() + 1, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverPercentageLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 90%");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(1.9, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverDurationLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 1h");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(3601, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(-4, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Sub_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 10.5)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 2.2");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(8.3, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 5.5)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(27.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 1.0)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5.5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(5.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 3.5)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 3");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(10.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 10)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 10)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 20");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 10)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 20.0");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(Map.of("_timestamp", 1,
                                                             "activeThreads", 10.5)))
                                        .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("_timestamp")
                                                                                       .dataType(IDataType.LONG.name())
                                                                                       .build(),
                                                      QueryResponse.QueryResponseColumn.builder()
                                                                                       .name("activeThreads")
                                                                                       .dataType(IDataType.DOUBLE.name())
                                                                                       .build()))
                                        .build());

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 3.0");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(3.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_VectorOverLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(5 + 5, values.getDouble(0), .0000000001);
        Assertions.assertEquals(20 + 5, values.getDouble(1), .0000000001);
        Assertions.assertEquals(25 + 5, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverLiteral_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + " 5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(5 - 5, values.getDouble(0), .0000000001);
        Assertions.assertEquals(20 - 5, values.getDouble(1), .0000000001);
        Assertions.assertEquals(25 - 5, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverLiteral_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(5 * 5, values.getDouble(0), .0000000001);
        Assertions.assertEquals(20 * 5, values.getDouble(1), .0000000001);
        Assertions.assertEquals(25 * 5, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverLiteral_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 24),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "5");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(5 / 5, values.getLong(0));
        Assertions.assertEquals(24 / 5, values.getLong(1));
        Assertions.assertEquals(25 / 5, values.getLong(2));

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverScalar_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 1.0)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 11)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(12, valCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 1.0)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 11)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(-10, valCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 2)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 11)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(22, valCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 55)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 11)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("value");
        Assertions.assertEquals(5, values.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverVector_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "totalThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 6),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(8, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(9, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(10, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "totalThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 6),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(-2, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(-3, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(-4, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "totalThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 6),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(15, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(18, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(21, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 100)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "totalThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(20, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(4, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Div_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 10)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "totalThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(2, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(0.5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(0.4, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Div_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "activeThreads", 10)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "totalThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(2, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(0.5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(0.4, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(10, values.getDouble(0), .0000000001);
        Assertions.assertEquals(25, values.getDouble(1), .0000000001);
        Assertions.assertEquals(30, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Add_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 20),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5.7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(10.7, values.getDouble(0), .0000000001);
        Assertions.assertEquals(25.7, values.getDouble(1), .0000000001);
        Assertions.assertEquals(30.7, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Add_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 5.5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 20.6),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 25.7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(10.5, values.getDouble(0), .0000000001);
        Assertions.assertEquals(25.6, values.getDouble(1), .0000000001);
        Assertions.assertEquals(30.7, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(-2, values.getDouble(0), .0000000001);
        Assertions.assertEquals(-1, values.getDouble(1), .0000000001);
        Assertions.assertEquals(0, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Sub_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5.5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(-2.5, values.getDouble(0), .0000000001);
        Assertions.assertEquals(-1.5, values.getDouble(1), .0000000001);
        Assertions.assertEquals(-0.5, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Sub_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3.5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4.5),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5.5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column values = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, values.size());
        Assertions.assertEquals(-1.5, values.getDouble(0), .0000000001);
        Assertions.assertEquals(-0.5, values.getDouble(1), .0000000001);
        Assertions.assertEquals(0.5, values.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(9, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(12, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(15, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Mul_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 3.5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(10.5, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(14, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(17.5, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Mul_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3.5),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4.5),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5.5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(10.5, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(13.5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(16.5, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 55),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 60),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 77)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 11)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(5, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(7, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Div_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 20),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 25),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 50)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 50)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(0.4, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(0.5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(1, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverScalar_Div_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 20),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 25),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 50)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1,
                                                                "totalThreads", 50)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        IntermediateQueryResult response = evaluator.execute().get();

        Column valCol = response.getTable().getColumn("activeThreads");
        Assertions.assertEquals(3, valCol.size());
        Assertions.assertEquals(0.4, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(0.5, valCol.getDouble(1), .0000000001);
        Assertions.assertEquals(1, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(3, dimCol.size());
        Assertions.assertEquals("app1", dimCol.getString(0));
        Assertions.assertEquals("app2", dimCol.getString(1));
        Assertions.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_VectorOverVector_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1 + 21, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5 + 32, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Add_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.5),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.6),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1 + 21.5, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5 + 32.6, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Add_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5.2),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.5),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.6),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1.1 + 21.5, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5.2 + 32.6, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1 - 21, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5 - 32, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Sub_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.2),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1 - 21.1, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5 - 32.2, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Sub_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5.5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9.9)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1.1 - 21, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5.5 - 32, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Sub_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1.4),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5.5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9.6)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.2),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1.4 - 21.1, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5.5 - 32.2, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1.4),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5.5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9.6)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.2),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1 * 21, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5 * 32, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Mul_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.2),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.3),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1 * 21.2, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5 * 32.3, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Mul_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5.2),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1.1 * 21, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5.2 * 32, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Mul_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 1.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 5.2),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 9.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 21.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 32.2),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 43.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(1.1 * 21.1, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(5.2 * 32.2, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 50),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 100),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 200)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 25),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 50),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 100)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(50.0 / 25, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(100.0 / 50, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Div_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 50),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 100),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 200)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 25.5),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 50.6),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 100.6)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(50 / 25.5, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(100 / 50.6, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Div_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 12),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 25),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 200)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 25),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 50),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 100)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals((double) 12 / 25, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals((double) 25 / 50, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_Div_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "activeThreads", 12.1),
                                                         Map.of("_timestamp", 2, "appName", "app3", "activeThreads", 25.2),
                                                         Map.of("_timestamp", 3, "appName", "app1", "activeThreads", 200.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app2", "totalThreads", 25.6),
                                                         Map.of("_timestamp", 2, "appName", "app3", "totalThreads", 50.7),
                                                         Map.of("_timestamp", 3, "appName", "app4", "totalThreads", 100.8)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valCol.size());
        Assertions.assertEquals(12.1 / 25.6, valCol.getDouble(0), .0000000001);
        Assertions.assertEquals(25.2 / 50.7, valCol.getDouble(1), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(2, dimCol.size());
        Assertions.assertEquals("app2", dimCol.getString(0));
        Assertions.assertEquals("app3", dimCol.getString(1));
    }

    @Test
    public void test_VectorOverVector_NoIntersection() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 12.1),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 25.2),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 200.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app4", "totalThreads", 25.6),
                                                         Map.of("_timestamp", 2, "appName", "app5", "totalThreads", 50.7),
                                                         Map.of("_timestamp", 3, "appName", "app6", "totalThreads", 100.8)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0, valCol.size());

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(0, dimCol.size());
    }

    /**
     * TODO: optimization, in this case, the final expression should be optimized to empty
     * 1 and 2 have no intersection
     */
    @Test
    public void test_VectorOverVector_NoIntersection_2() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 12.1),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 25.2),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 200.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 4, "appName", "app4", "totalThreads", 25.6),
                                                         Map.of("_timestamp", 5, "appName", "app5", "totalThreads", 50.7),
                                                         Map.of("_timestamp", 6, "appName", "app6", "totalThreads", 100.8)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   if ("newThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 6, "appName", "app6", "newThreads", 106),
                                                         Map.of("_timestamp", 7, "appName", "app7", "newThreads", 107)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("newThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0, valCol.size());

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(0, dimCol.size());
    }

    /**
     * TODO: optimization, in this case, the final expression should be optimized to empty
     * 1 and 2 have intersection while 2 and 3 has no intersection
     */
    @Test
    public void test_VectorOverVector_NoIntersection_3() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 12.1),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 25.2),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 200.3)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 25.6),
                                                         Map.of("_timestamp", 4, "appName", "app4", "totalThreads", 50.7)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   if ("newThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 6, "appName", "app6", "newThreads", 106),
                                                         Map.of("_timestamp", 7, "appName", "app7", "newThreads", 107)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("newThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0, valCol.size());

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(0, dimCol.size());
    }

    @Test
    public void test_MultipleExpressions() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if ("activeThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   if ("totalThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 2, "appName", "app2", "totalThreads", 25),
                                                         Map.of("_timestamp", 3, "appName", "app3", "totalThreads", 26),
                                                         Map.of("_timestamp", 4, "appName", "app4", "totalThreads", 27)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("totalThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   if ("newThreads".equals(metric)) {
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 3, "appName", "app3", "newThreads", 35),
                                                         Map.of("_timestamp", 4, "appName", "app4", "newThreads", 36),
                                                         Map.of("_timestamp", 5, "appName", "app5", "newThreads", 37)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("newThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
                                                      + "/ "
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
                                                      + "* "
                                                      + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
                                                      + "+ 5");
        IntermediateQueryResult response = evaluator.execute().get();

        // Only the overlapped series will be returned
        Column valCol = response.getTable().getColumn("value");
        Assertions.assertEquals(1, valCol.size());
        Assertions.assertEquals(5.0 / 26 * 35 + 5, valCol.getDouble(0), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assertions.assertEquals(1, dimCol.size());
        Assertions.assertEquals("app3", dimCol.getString(0));
    }

    @Test
    public void test_RelativeComparison() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest req = answer.getArgument(0, QueryRequest.class);

                   HumanReadableDuration offset = req.getOffset();
                   if (offset == null) {
                       String name = req.getFields().get(0).getName();
                       return QueryResponse.builder()
                                           .data(List.of(Map.of("_timestamp", 1, "appName", "app1", "activeThreads", 3),
                                                         Map.of("_timestamp", 2, "appName", "app2", "activeThreads", 4),
                                                         Map.of("_timestamp", 3, "appName", "app3", "activeThreads", 5)))
                                           .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("_timestamp")
                                                                                          .dataType(IDataType.LONG.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("appName")
                                                                                          .dataType(IDataType.STRING.name())
                                                                                          .build(),
                                                         QueryResponse.QueryResponseColumn.builder()
                                                                                          .name("activeThreads")
                                                                                          .dataType(IDataType.DOUBLE.name())
                                                                                          .build()))
                                           .build();
                   }

                   String name = req.getFields().get(0).getName();
                   return QueryResponse.builder()
                                       .data(List.of(Map.of("_timestamp", 2, "appName", "app2", "-1d", 21),
                                                     Map.of("_timestamp", 3, "appName", "app3", "-1d", 22),
                                                     Map.of("_timestamp", 4, "appName", "app4", "-1d", 23)))
                                       .meta(List.of(QueryResponse.QueryResponseColumn.builder()
                                                                                      .name("_timestamp")
                                                                                      .dataType(IDataType.LONG.name())
                                                                                      .build(),
                                                     QueryResponse.QueryResponseColumn.builder()
                                                                                      .name("appName")
                                                                                      .dataType(IDataType.STRING.name())
                                                                                      .build(),
                                                     QueryResponse.QueryResponseColumn.builder()
                                                                                      .name("-1d")
                                                                                      .dataType(IDataType.DOUBLE.name())
                                                                                      .build()))
                                       .build();

               });

        IQueryStep evaluator = QueryPipelineBuilder.builder()
                                                   .dataSourceApi(dataSourceApi)
                                                   .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                                   // BY is given so that it produces a vector
                                                   .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName) > -5%[-1d]");
        IntermediateQueryResult response = evaluator.execute().get();
        Assertions.assertEquals(2, response.getRows());

        // Only the overlapped series(app2,app3) will be returned
        {
            Column valCol = response.getTable().getColumn("activeThreads");
            Assertions.assertEquals(2, valCol.size());
            Assertions.assertEquals(4, valCol.getDouble(0), .0000000001);
            Assertions.assertEquals(5, valCol.getDouble(1), .0000000001);
        }
        {
            Column valCol = response.getTable().getColumn("-1d");
            Assertions.assertEquals(2, valCol.size());
            Assertions.assertEquals(21, valCol.getDouble(0), .0000000001);
            Assertions.assertEquals(22, valCol.getDouble(1), .0000000001);
        }
        {
            Column valCol = response.getTable().getColumn("delta");
            Assertions.assertEquals(2, valCol.size());
            Assertions.assertEquals((4.0 - 21) / 21, valCol.getDouble(0), .0000000001);
            Assertions.assertEquals((5.0 - 22) / 22, valCol.getDouble(1), .0000000001);
        }
    }
}
