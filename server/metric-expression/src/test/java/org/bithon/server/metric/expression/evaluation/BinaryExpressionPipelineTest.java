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


import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.server.web.service.datasource.api.ColumnarResponse;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:27 pm
 */
public class BinaryExpressionPipelineTest {
    private IDataSourceApi dataSourceApi;

    @Before
    public void setUpClass() {
        dataSourceApi = Mockito.mock(IDataSourceApi.class);
    }

    @Test
    public void test_ScalarOverLiteral_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(6, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverSizeLiteral_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5Mi");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(HumanReadableNumber.of("5Mi").longValue() + 1, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverPercentageLiteral_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 90%");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(1.9, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverDurationLiteral_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 1h");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3601, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Sub() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(-4, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1))))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(0.2, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverLiteral_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
                                                                              "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                              ))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) + 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(6, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(7, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(8, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverLiteral_Sub() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
                                                                              "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                              ))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) - 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(-4, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(-3, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(-2, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverLiteral_Mul() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
                                                                              "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                              ))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) * 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(10, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(15, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverLiteral_Div() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
                                                                              "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                              ))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName) / 5");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(0.2, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(0.4, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(0.6, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_LiteralOverVector_Sub() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 3.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
                                                                              "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                              ))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("5 - avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName)");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(4, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(3, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(2, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_LiteralOverVector_Div() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(ColumnarResponse.builder()
                                                              .values(List.of("activeThreads"))
                                                              .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0, 2.0, 4.0)),
                                                                              "_timestamp", new ArrayList<>(List.of(1, 2, 3)),
                                                                              "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                              ))
                                                              .build())
                                        .build());

        IEvaluator evaluator = EvaluatorBuilder.builder()
                                               .dataSourceApi(dataSourceApi)
                                               .intervalRequest(IntervalRequest.builder()
                                                                               .bucketCount(1)
                                                                               .build())
                                               // BY is given so that it produces a vector
                                               .build("5 / avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by(appName)");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(2.5, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(1.25, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(11))))
                                                                 .build())
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
        ColumnarResponse response = evaluator.evaluate().get();

        // Use the name of left expression as the output column name
        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(12, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Sub() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(1.0))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(11))))
                                                                 .build())
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
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(-10, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Mul() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(2.0))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(11))))
                                                                 .build())
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
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(22, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverScalar_Div() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(55))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(11))))
                                                                 .build())
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
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverScalar_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(5))))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(8, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(9, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(10, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverScalar_Sub() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(5))))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(-2, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(-1, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(0, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverScalar_Mul() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(3, 4, 5)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(3))))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(9, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(12, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(15, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_VectorOverScalar_Div() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   QueryRequest request = answer.getArgument(0, QueryRequest.class);

                   String metric = request.getFields().get(0).getName();
                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(55, 121, 66)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(11))))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(5, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(11, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(6, ((Number) values.get(2)).doubleValue(), .0000000001);
    }


    @Test
    public void test_ScalarOverVector_Add() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(3))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(5, 6, 7)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                                 ))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "+"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(8, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(9, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(10, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverVector_Sub() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(3))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(5, 6, 7)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                                 ))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "-"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(-2, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(-3, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(-4, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverVector_Mul() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(3))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(5, 6, 7)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                                 ))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "*"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(15, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(18, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(21, ((Number) values.get(2)).doubleValue(), .0000000001);
    }

    @Test
    public void test_ScalarOverVector_Div() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   String metric = answer.getArgument(0, QueryRequest.class)
                                         .getFields()
                                         .get(0).getName();

                   if (metric.equals("activeThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .values(List.of("activeThreads"))
                                                                 .columns(Map.of("activeThreads", new ArrayList<>(List.of(100))))
                                                                 .build()
                                           )
                                           .build();
                   }
                   if (metric.equals("totalThreads")) {
                       return QueryResponse.builder()
                                           .data(ColumnarResponse.builder()
                                                                 .keys(List.of("appName"))
                                                                 .values(List.of("totalThreads"))
                                                                 .columns(Map.of("totalThreads", new ArrayList<>(List.of(5, 20, 25)),
                                                                                 "appName", new ArrayList<>(List.of("app1", "app2", "app3"))
                                                                 ))
                                                                 .build())
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
                                               .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                      + "/"
                                                      + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        ColumnarResponse response = evaluator.evaluate().get();

        List<Object> values = response.getColumns().get("activeThreads");
        Assert.assertEquals(3, values.size());
        Assert.assertEquals(20, ((Number) values.get(0)).doubleValue(), .0000000001);
        Assert.assertEquals(4, ((Number) values.get(1)).doubleValue(), .0000000001);
        Assert.assertEquals(5, ((Number) values.get(2)).doubleValue(), .0000000001);
    }
}
