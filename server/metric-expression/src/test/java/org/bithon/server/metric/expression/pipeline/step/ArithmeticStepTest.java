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

package org.bithon.server.metric.expression.pipeline.step;


import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.result.Column;
import org.bithon.server.datasource.query.result.ColumnarTable;
import org.bithon.server.datasource.query.result.DoubleColumn;
import org.bithon.server.datasource.query.result.LongColumn;
import org.bithon.server.datasource.query.result.PipelineQueryResult;
import org.bithon.server.datasource.query.result.StringColumn;
import org.bithon.server.metric.expression.pipeline.PhysicalPlanner;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.IntervalRequest;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:27 pm
 */
@SuppressWarnings("PointlessArithmeticExpression")
public class ArithmeticStepTest {
    private IDataSourceApi dataSourceApi;

    @BeforeEach
    public void setUpClass() {
        dataSourceApi = Mockito.mock(IDataSourceApi.class);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(6, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 3.3");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(4.3, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 3.7)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(8.7, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Add_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 10.5)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 2.2");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(12.7, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverSizeLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5Mi");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(HumanReadableNumber.of("5Mi").longValue() + 1, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverPercentageLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 90%");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(1.9, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverDurationLiteral_Add_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 1h");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(3601, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Sub_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(-4, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Sub_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 10.5)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 2.2");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(8.3, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 5.5)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(27.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 1)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 5.5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(5.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Mul_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 3.5)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * 3");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(10.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Long_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 10)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 5");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(2, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Double_Long() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 10)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 20");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Long_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            LongColumn.of("activeThreads", 10)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 20.0");
        PipelineQueryResult response = evaluator.execute().get();

        Column valueCol = response.getTable().getColumn("value");
        Assertions.assertEquals(0.5, valueCol.getDouble(0), .0000000001);
    }

    @Test
    public void test_ScalarOverLiteral_Div_Double_Double() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenReturn(ColumnarTable.of(LongColumn.of("_timestamp", 1),
                                            DoubleColumn.of("activeThreads", 10.5)));

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 3.0");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 5, 20, 25)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "5");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 5, 20, 25)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + " 5");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 5, 20, 25)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "5");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 5, 24, 25)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "5");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 1)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 11)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 1)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 11)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 2)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 11)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 55)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 11)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("totalThreads", 5, 6, 7)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("totalThreads", 5, 6, 7)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("totalThreads", 5, 6, 7)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("activeThreads", 100)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("totalThreads", 5, 20, 25)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           DoubleColumn.of("activeThreads", 10)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("totalThreads", 5, 20, 25)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           DoubleColumn.of("activeThreads", 10)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("totalThreads", 5, 20, 25)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m]"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]  by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 5, 20, 25)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 5, 20, 25)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           DoubleColumn.of("totalThreads", 5.7)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 5.5, 20.6, 25.7)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 3, 4, 5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 3, 4, 5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           DoubleColumn.of("totalThreads", 5.5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 3.5, 4.5, 5.5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 3, 4, 5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 3)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 3, 4, 5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           DoubleColumn.of("totalThreads", 3.5)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 3.5, 4.5, 5.5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 3)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 55, 60, 77)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 11)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           LongColumn.of("activeThreads", 20, 25, 50)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           DoubleColumn.of("totalThreads", 50.0)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 20, 25, 50)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1),
                           LongColumn.of("totalThreads", 50)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m]");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 1, 5, 9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 21, 32, 43)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 1, 5, 9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 21.5, 32.6, 43.7)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 1.1, 5.2, 9.3)
                       );

                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 21.5, 32.6, 43.7)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 1, 5, 9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 21, 32, 43)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 1, 5, 9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 21.1, 32.2, 43.3)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 1.1, 5.5, 9.9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 21, 32, 43)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 1.4, 5.5, 9.6)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 21.1, 32.2, 43.3)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "-"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 1, 5, 9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 21, 32, 43)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 1, 5, 9)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 21.2, 32.3, 43.3)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 1.1, 5.2, 9.3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 21, 32, 43)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 1.1, 5.2, 9.3)
                       );

                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 21.1, 32.2, 43.3)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "*"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 50, 100, 200)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 25, 50, 100)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           LongColumn.of("activeThreads", 50, 100, 200)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 25.5, 50.6, 100.6)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 12, 25, 200)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           LongColumn.of("totalThreads", 25, 50, 100)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app1"),
                           DoubleColumn.of("activeThreads", 12.1, 25.2, 200.3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 25.6, 50.7, 100.8)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 12.1, 25.2, 200.3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app4", "app5", "app6"),
                           DoubleColumn.of("totalThreads", 25.6, 50.7, 100.8)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 12.1, 25.2, 200.3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 4, 5, 6),
                           StringColumn.of("appName", "app4", "app5", "app6"),
                           DoubleColumn.of("totalThreads", 25.6, 50.7, 100.8)
                       );
                   }

                   if ("newThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 6, 7),
                           StringColumn.of("appName", "app6", "app7"),
                           DoubleColumn.of("newThreads", 106, 107)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 12.1, 25.2, 200.3)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 3, 4),
                           StringColumn.of("appName", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 25.6, 50.7)
                       );
                   }

                   if ("newThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 6, 7),
                           StringColumn.of("appName", "app6", "app7"),
                           DoubleColumn.of("newThreads", 106, 107)
                       );
                   }

                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "/"
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName)"
                                                          + "+"
                                                          + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName)");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 3, 4, 5)
                       );
                   }
                   if ("totalThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 2, 3, 4),
                           StringColumn.of("appName", "app2", "app3", "app4"),
                           DoubleColumn.of("totalThreads", 25, 26, 27)
                       );
                   }

                   if ("newThreads".equals(metric)) {
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 3, 4, 5),
                           StringColumn.of("appName", "app3", "app4", "app5"),
                           DoubleColumn.of("newThreads", 35, 36, 37)
                       );
                   }
                   throw new IllegalArgumentException("Invalid metric: " + metric);
               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
                                                          + "/ "
                                                          + "avg(jvm-metrics.totalThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
                                                          + "* "
                                                          + "avg(jvm-metrics.newThreads{appName = \"bithon-web-'local\"})[1m] by (appName) "
                                                          + "+ 5");
        PipelineQueryResult response = evaluator.execute().get();

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
                       return ColumnarTable.of(
                           LongColumn.of("_timestamp", 1, 2, 3),
                           StringColumn.of("appName", "app1", "app2", "app3"),
                           DoubleColumn.of("activeThreads", 3, 4, 5)
                       );
                   }

                   return ColumnarTable.of(
                       LongColumn.of("_timestamp", 2, 3, 4),
                       StringColumn.of("appName", "app2", "app3", "app4"),
                       DoubleColumn.of("-1d", 21, 22, 23)
                   );

               });

        IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                 .dataSourceApi(dataSourceApi)
                                                 .interval(IntervalRequest.builder()
                                                                          .bucketCount(1)
                                                                          .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                          .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                          .build())
                                                 // BY is given so that it produces a vector
                                                 .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] by (appName) > -5%[-1d]");
        PipelineQueryResult response = evaluator.execute()
                                                .get();
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

    @Test
    public void test_FilterStep_Double_GT() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   return ColumnarTable.of(
                       LongColumn.of("_timestamp", 1, 2, 3),
                       StringColumn.of("appName", "app1", "app2", "app3"),
                       DoubleColumn.of("activeThreads", 3, 4, 5)
                   );
               });

        //
        // Case 1, > 2
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     // BY is given so that it produces a vector
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 2");
            PipelineQueryResult response = evaluator.execute().get();

            // all 3 records satisfy the filter condition
            Assertions.assertEquals(3, response.getRows());

            // Only the overlapped series(app2,app3) will be returned
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(3, valCol.size());
                Assertions.assertEquals(3, valCol.getDouble(0), .0000000001);
                Assertions.assertEquals(4, valCol.getDouble(1), .0000000001);
                Assertions.assertEquals(5, valCol.getDouble(2), .0000000001);
            }
        }

        //
        // Case 2, > 3, two rows satisfy the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     // BY is given so that it produces a vector
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 3");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(2, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(2, valCol.size());
                Assertions.assertEquals(4, valCol.getDouble(0), .0000000001);
                Assertions.assertEquals(5, valCol.getDouble(1), .0000000001);
            }
        }

        //
        // Case 3, > 4, 1 rows satisfies the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     // BY is given so that it produces a vector
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 4");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(1, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(1, valCol.size());
                Assertions.assertEquals(5, valCol.getDouble(0), .0000000001);
            }
        }


        //
        // Case 4, > 5, 0 rows satisfies the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     // BY is given so that it produces a vector
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 5");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(0, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(0, valCol.size());
            }
        }
    }

    @Test
    public void test_FilterStep_Double_GTE() throws Exception {
        Mockito.when(dataSourceApi.timeseriesV5(Mockito.any()))
               .thenAnswer((answer) -> {
                   return ColumnarTable.of(
                       LongColumn.of("_timestamp", 1, 2, 3),
                       StringColumn.of("appName", "app1", "app2", "app3"),
                       DoubleColumn.of("activeThreads", 3, 4, 5)
                   );
               });

        //
        // Case 1, >= 2, all 3 rows satisfy the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] >= 2");
            PipelineQueryResult response = evaluator.execute().get();

            // all 3 records satisfy the filter condition
            Assertions.assertEquals(3, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(3, valCol.size());
                Assertions.assertEquals(3, valCol.getDouble(0), .0000000001);
                Assertions.assertEquals(4, valCol.getDouble(1), .0000000001);
                Assertions.assertEquals(5, valCol.getDouble(2), .0000000001);
            }
        }

        //
        // Case 2, >= 3, all 3 rows satisfy the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] >= 3");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(3, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(3, valCol.size());
                Assertions.assertEquals(3, valCol.getDouble(0), .0000000001);
                Assertions.assertEquals(4, valCol.getDouble(1), .0000000001);
                Assertions.assertEquals(5, valCol.getDouble(2), .0000000001);
            }
        }

        //
        // Case 3, >= 4, 2 rows satisfies the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     // BY is given so that it produces a vector
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] >= 4");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(2, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(2, valCol.size());
                Assertions.assertEquals(4, valCol.getDouble(0), .0000000001);
                Assertions.assertEquals(5, valCol.getDouble(1), .0000000001);
            }
        }

        //
        // Case 4, >= 5, some rows satisfy the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     // BY is given so that it produces a vector
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] >= 5");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(1, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(1, valCol.size());
                Assertions.assertEquals(5, valCol.getDouble(0), .0000000001);
            }
        }

        //
        // Case 5, >= 6, 0 rows satisfies the filter condition
        //
        {
            IPhysicalPlan evaluator = PhysicalPlanner.builder()
                                                     .dataSourceApi(dataSourceApi)
                                                     .interval(IntervalRequest.builder()
                                                                              .bucketCount(1)
                                                                              .startISO8601(TimeSpan.fromISO8601("2023-01-01T00:00:00+08:00"))
                                                                              .endISO8601(TimeSpan.fromISO8601("2023-01-01T00:01:00+08:00"))
                                                                              .build())
                                                     .build("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] >= 6");
            PipelineQueryResult response = evaluator.execute().get();

            Assertions.assertEquals(0, response.getRows());
            {
                Column valCol = response.getTable().getColumn("activeThreads");
                Assertions.assertEquals(0, valCol.size());
            }
        }
    }
}
