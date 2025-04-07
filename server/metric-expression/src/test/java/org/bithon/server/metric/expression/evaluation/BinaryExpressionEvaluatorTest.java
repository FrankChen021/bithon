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

package org.bithon.server.metric.expression.evaluation;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:27 pm
 */
public class BinaryExpressionEvaluatorTest {
    private IDataSourceApi dataSourceApi;

    @Before
    public void setUpClass() {
        dataSourceApi = Mockito.mock(IDataSourceApi.class);
    }

    @Test
    public void test_ScalarOverLiteral_Add() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(6, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverSizeLiteral_Add() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5Mi");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(HumanReadableNumber.of("5Mi").longValue() + 1, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverPercentageLiteral_Add() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 90%");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(1.9, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverDurationLiteral_Add() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 1h");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(3601, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Sub() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 5");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(-4, valueCol.getDouble(0), .0000000001);
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(5, valueCol.getDouble(0), .0000000001);
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(27.5, valueCol.getDouble(0), .0000000001);
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5.5");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(5.5, valueCol.getDouble(0), .0000000001);
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 5");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(2, valueCol.getDouble(0), .0000000001);
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 20");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(0.5, valueCol.getDouble(0), .0000000001);
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 20.0");
        EvaluationResult response = evaluator.evaluate().get();

        Column valueCol = response.getTable().getColumn("value");
        Assert.assertEquals(0.5, valueCol.getDouble(0), .0000000001);
    }

    //
//    @Test
//    public void test_VectorOverLiteral_Add() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenReturn(QueryResponse.builder()
//                                        .data(EvaluationResult.builder()
//                                                              .valueNames(List.of("activeThreads"))
//                                                              .values(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
//                                                                             "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
//                                                                             "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
//                                                              ))
//                                                              .build())
//                                        .build());
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) + 5");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(6, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(7, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(8, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverLiteral_Sub() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenReturn(QueryResponse.builder()
//                                        .data(EvaluationResult.builder()
//                                                              .valueNames(List.of("activeThreads"))
//                                                              .values(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
//                                                                             "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
//                                                                             "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
//                                                              ))
//                                                              .build())
//                                        .build());
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) - 5");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(-4, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(-3, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(-2, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverLiteral_Mul() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenReturn(QueryResponse.builder()
//                                        .data(EvaluationResult.builder()
//                                                              .valueNames(List.of("activeThreads"))
//                                                              .values(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
//                                                                             "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
//                                                                             "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
//                                                              ))
//                                                              .build())
//                                        .build());
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) * 5");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(10, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(15, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverLiteral_Div() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenReturn(QueryResponse.builder()
//                                        .data(EvaluationResult.builder()
//                                                              .valueNames(List.of("activeThreads"))
//                                                              .values(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
//                                                                             "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
//                                                                             "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
//                                                              ))
//                                                              .build())
//                                        .build());
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) / 5");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(0.2, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(0.4, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(0.6, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_LiteralOverVector_Sub() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenReturn(QueryResponse.builder()
//                                        .data(EvaluationResult.builder()
//                                                              .valueNames(List.of("activeThreads"))
//                                                              .values(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
//                                                                             "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
//                                                                             "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
//                                                              ))
//                                                              .build())
//                                        .build());
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("5 - avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(4, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(3, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(2, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_LiteralOverVector_Div() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenReturn(QueryResponse.builder()
//                                        .data(EvaluationResult.builder()
//                                                              .valueNames(List.of("activeThreads"))
//                                                              .values(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 4.0)),
//                                                                             "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
//                                                                             "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
//                                                              ))
//                                                              .build())
//                                        .build());
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("5 / avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(2.5, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(1.25, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
    @Test
    public void test_ScalarOverScalar_Add() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("value");
        Assert.assertEquals(12, valCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Sub() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("value");
        Assert.assertEquals(-10, valCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Mul() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("value");
        Assert.assertEquals(22, valCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Div() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        EvaluationResult response = evaluator.evaluate().get();

        Column values = response.getTable().getColumn("value");
        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverVector_Add() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assert.assertEquals(3, valCol.size());
        Assert.assertEquals(8, valCol.getDouble(0), .0000000001);
        Assert.assertEquals(9, valCol.getDouble(1), .0000000001);
        Assert.assertEquals(10, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assert.assertEquals(3, dimCol.size());
        Assert.assertEquals("app1", dimCol.getString(0));
        Assert.assertEquals("app2", dimCol.getString(1));
        Assert.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Sub() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assert.assertEquals(3, valCol.size());
        Assert.assertEquals(-2, valCol.getDouble(0), .0000000001);
        Assert.assertEquals(-3, valCol.getDouble(1), .0000000001);
        Assert.assertEquals(-4, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assert.assertEquals(3, dimCol.size());
        Assert.assertEquals("app1", dimCol.getString(0));
        Assert.assertEquals("app2", dimCol.getString(1));
        Assert.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Mul() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assert.assertEquals(3, valCol.size());
        Assert.assertEquals(15, valCol.getDouble(0), .0000000001);
        Assert.assertEquals(18, valCol.getDouble(1), .0000000001);
        Assert.assertEquals(21, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assert.assertEquals(3, dimCol.size());
        Assert.assertEquals("app1", dimCol.getString(0));
        Assert.assertEquals("app2", dimCol.getString(1));
        Assert.assertEquals("app3", dimCol.getString(2));
    }

    @Test
    public void test_ScalarOverVector_Div() throws Exception {
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

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        EvaluationResult response = evaluator.evaluate().get();

        Column valCol = response.getTable().getColumn("totalThreads");
        Assert.assertEquals(3, valCol.size());
        Assert.assertEquals(20, valCol.getDouble(0), .0000000001);
        Assert.assertEquals(5, valCol.getDouble(1), .0000000001);
        Assert.assertEquals(4, valCol.getDouble(2), .0000000001);

        Column dimCol = response.getTable().getColumn("appName");
        Assert.assertEquals(3, dimCol.size());
        Assert.assertEquals("app1", dimCol.getString(0));
        Assert.assertEquals("app2", dimCol.getString(1));
        Assert.assertEquals("app3", dimCol.getString(2));
    }


//
//    @Test
//    public void test_VectorOverScalar_Add() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   QueryRequest request = answer.getArgument(0, QueryRequest.class);
//
//                   String metric = request.getFields().get(0).getName();
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
//                                                                                "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(5))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "+"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(3, values.size());
//        Assert.assertEquals(8, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(9, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(10, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverScalar_Sub() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   QueryRequest request = answer.getArgument(0, QueryRequest.class);
//
//                   String metric = request.getFields().get(0).getName();
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
//                                                                                "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(5))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "-"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(3, values.size());
//        Assert.assertEquals(-2, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(-1, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(0, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverScalar_Mul() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   QueryRequest request = answer.getArgument(0, QueryRequest.class);
//
//                   String metric = request.getFields().get(0).getName();
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
//                                                                                "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(3))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "*"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(3, values.size());
//        Assert.assertEquals(9, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(12, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(15, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverScalar_Div() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   QueryRequest request = answer.getArgument(0, QueryRequest.class);
//
//                   String metric = request.getFields().get(0).getName();
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(55, 121, 66)),
//                                                                                "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(11))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "/"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        List<Object> values = response.getValues().get("activeThreads");
//        Assert.assertEquals(3, values.size());
//        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(11, ((Number) values.get(1)).doubleValue(), .0000000001);
//        Assert.assertEquals(6, ((Number) values.get(2)).doubleValue(), .0000000001);
//    }
//
//
//    @Test
//    public void test_VectorOverVector_Add() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   String metric = answer.getArgument(0, QueryRequest.class)
//                                         .getFields()
//                                         .get(0).getName();
//
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app1")))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(1, 5, 9))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app4")))
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(21, 32, 43))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "+"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//
//        // Only the overlapped series will be returned
//        List<Object> values = response.getValues().get("value");
//        Assert.assertEquals(2, values.size());
//        Assert.assertEquals(1 + 21, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(5 + 32, ((Number) values.get(1)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverVector_Sub() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   String metric = answer.getArgument(0, QueryRequest.class)
//                                         .getFields()
//                                         .get(0).getName();
//
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app1"), List.of("app2"), List.of("app3")))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app4")))
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(21, 22, 23))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "-"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//
//        // Only the overlapped series will be returned
//        List<Object> values = response.getValues().get("value");
//        Assert.assertEquals(2, values.size());
//        Assert.assertEquals(4 - 21, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(5 - 22, ((Number) values.get(1)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverVector_Mul() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   String metric = answer.getArgument(0, QueryRequest.class)
//                                         .getFields()
//                                         .get(0).getName();
//
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app1"), List.of("app2"), List.of("app3")))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app4")))
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(21, 22, 23))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "*"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//
//        // Only the overlapped series will be returned
//        List<Object> values = response.getValues().get("value");
//        Assert.assertEquals(2, values.size());
//        Assert.assertEquals(4 * 21, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(5 * 22, ((Number) values.get(1)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverVector_Div() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   String metric = answer.getArgument(0, QueryRequest.class)
//                                         .getFields()
//                                         .get(0).getName();
//
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app1"), List.of("app2"), List.of("app3")))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
//                                                                                "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app4")))
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(21, 22, 23))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "/"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//
//        // Only the overlapped series will be returned
//        List<Object> values = response.getValues().get("value");
//        Assert.assertEquals(2, values.size());
//        Assert.assertEquals(4.0 / 21, ((Number) values.get(0)).doubleValue(), .0000000001);
//        Assert.assertEquals(5.0 / 22, ((Number) values.get(1)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_VectorOverVector_NoIntersection() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   String metric = answer.getArgument(0, QueryRequest.class)
//                                         .getFields()
//                                         .get(0).getName();
//
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app1"), List.of("app2"), List.of("app3")))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app4"), List.of("app5"), List.of("app6")))
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(21, 22, 23))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
//                                                      + "/"
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//        Assert.assertEquals(0, response.getRows());
//        List<Object> values = response.getValues().get("value");
//        Assert.assertEquals(0, values.size());
//    }
//
//    @Test
//    public void test_MultipleExpressions() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   String metric = answer.getArgument(0, QueryRequest.class)
//                                         .getFields()
//                                         .get(0).getName();
//
//                   if ("activeThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app1"), List.of("app2"), List.of("app3")))
//                                                                 .valueNames(List.of("activeThreads"))
//                                                                 .values(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//                   if ("totalThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app4")))
//                                                                 .valueNames(List.of("totalThreads"))
//                                                                 .values(Map.of("totalThreads", new ArrayList<>(List.of(21, 22, 23))))
//                                                                 .build())
//                                           .build();
//                   }
//
//                   if ("newThreads".equals(metric)) {
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app3"), List.of("app4"), List.of("app5")))
//                                                                 .valueNames(List.of("newThreads"))
//                                                                 .values(Map.of("newThreads", new ArrayList<>(List.of(101, 102, 103))))
//                                                                 .build())
//                                           .build();
//                   }
//                   throw new IllegalArgumentException("Invalid metric: " + metric);
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
//                                                      + "/ "
//                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
//                                                      + "* "
//                                                      + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
//                                                      + "+ 5");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//
//        // Only the overlapped series will be returned
//        List<Object> values = response.getValues().get("value");
//        Assert.assertEquals(1, values.size());
//        Assert.assertEquals(5.0 / 22 * 101 + 5, ((Number) values.get(0)).doubleValue(), .0000000001);
//    }
//
//    @Test
//    public void test_RelativeComparison() throws Exception {
//        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
//               .thenAnswer((answer) -> {
//                   QueryRequest req = answer.getArgument(0, QueryRequest.class);
//
//                   HumanReadableDuration offset = req.getOffset();
//                   if (offset == null) {
//                       String name = req.getFields().get(0).getName();
//                       return QueryResponse.builder()
//                                           .data(EvaluationResult.builder()
//                                                                 .keyNames(List.of("appName"))
//                                                                 .keys(List.of(List.of("app2"), List.of("app3"), List.of("app1")))
//                                                                 .valueNames(List.of(name))
//                                                                 .values(Map.of(name, new ArrayList<>(List.of(3, 4, 5))))
//                                                                 .build()
//                                           )
//                                           .build();
//                   }
//
//                   String name = req.getFields().get(0).getName();
//                   return QueryResponse.builder()
//                                       .data(EvaluationResult.builder()
//                                                             .keyNames(List.of("appName"))
//                                                             .keys(List.of(List.of("app2"), List.of("app3"), List.of("app4")))
//                                                             .valueNames(List.of(name))
//                                                             .values(Map.of(name, new ArrayList<>(List.of(21, 22, 23))))
//                                                             .build())
//                                       .build();
//
//               });
//
//        IEvaluator evaluator = EvaluatorBuilder.builder()
//                                               .dataSourceApi(dataSourceApi)
//                                               .intervalRequest(IntervalRequest.builder()
//                                                                               .bucketCount(1)
//                                                                               .build())
//                                               // BY is given so that it produces a vector
//                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName) > -5%[-1d]");
//        EvaluationResult response = evaluator.evaluate().get();
//
//        Assert.assertArrayEquals(new String[]{"appName"}, response.getKeyNames());
//
//        // Only the overlapped series(app2,app3) will be returned
//        {
//            List<Object> values = response.getValues().get("curr");
//            Assert.assertEquals(2, values.size());
//            Assert.assertEquals(3, ((Number) values.get(0)).doubleValue(), .0000000001);
//            Assert.assertEquals(4, ((Number) values.get(1)).doubleValue(), .0000000001);
//        }
//        {
//            List<Object> values = response.getValues().get("base");
//            Assert.assertEquals(2, values.size());
//            Assert.assertEquals(21, ((Number) values.get(0)).doubleValue(), .0000000001);
//            Assert.assertEquals(22, ((Number) values.get(1)).doubleValue(), .0000000001);
//        }
//        {
//            List<Object> values = response.getValues().get("delta");
//            Assert.assertEquals(2, values.size());
//            Assert.assertEquals((3.0 - 21) / 21, ((Number) values.get(0)).doubleValue(), .0000000001);
//            Assert.assertEquals((4.0 - 22) / 22, ((Number) values.get(1)).doubleValue(), .0000000001);
//        }
//    }
}
