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

package org.bithon.server.alerting.evaluator.evaluator.pipeline;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 28/3/22 9:42 PM
 */
@Slf4j
public class ExpressionEvaluationStepTest {

    private static final IEvaluationLogWriter CONSOLE_LOGGER = new IEvaluationLogWriter() {
        @Override
        public void close() {
        }

        @Override
        public void setInstance(String instance) {

        }

        @Override
        public void write(EvaluationLogEvent logEvent) {
        }

        @Override
        public void write(List<EvaluationLogEvent> logs) {

        }
    };

    private final String metric = "count";

    private IDataSourceApi dataSourceProvider;

    @Before
    public void setUp() {
        ISchema schema = new DefaultSchema("test-metrics",
                                           "test-metrics",
                                           new TimestampSpec("timestamp"),
                                           Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                           Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        dataSourceProvider = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceProvider.getSchemaByName(schema.getName()))
               .thenReturn(schema);

    }

    @Test
    public void test_GreaterThan() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a metric that DOES satisfy the condition
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());

        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalAnd_AllConditionsSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a metric that DOES satisfy the condition
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 4 AND sum(test-metrics.%s)[1m] > 3", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalAnd_AllConditionsNOTSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 6)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build());


        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 6 AND sum(test-metrics.%s)[1m] > 6", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertFalse(context.isExpressionEvaluatedAsTrue());

        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalAnd_FirstConditionsNOTSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 6 AND sum(test-metrics.%s)[1m] > 3", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertFalse(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalAnd_2ndConditionsNOTSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 3 AND sum(test-metrics.%s)[1m] > 6", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);
        Assert.assertFalse(context.isExpressionEvaluatedAsTrue());

        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalOr_AllConditionSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 2 OR sum(test-metrics.%s)[1m] = 3", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());

        // Evaluation of 2nd expression will be skipped
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalOr_1stConditionSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 2 OR sum(test-metrics.%s)[1m] = 3", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalOr_2ndConditionSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 5 OR sum(test-metrics.%s)[1m] = 3", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LogicalOr_AllConditionNOTSatisfy() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 5 OR sum(test-metrics.%s)[1m] = 2", metric, metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertFalse(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GreaterThanOrEqual() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] >= 5", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LessThan() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] < 6", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_LessThanOrEqual() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] <= 5", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }


    @Test
    public void test_IsNull_OnEmptyMap() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.emptyList())
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] is null", metric);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_IsNull_OnNullReturn() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(null)
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] is null", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);

        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void testRelativeComparison_GTE() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] >= 100%%[-1m]", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void testRelativeComparison_GT() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 99%%[-1m]", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void testRelativeComparison_LT() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               // Current
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
               // Previous
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] < -50%%[-1m]", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is < 50%, Not triggered
        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertFalse(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void testRelativeComparison_LTE() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               // Current
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
               // Previous
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] <= 50%%[-1m]", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is <= 50%, triggered
        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider,
                                                          null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_Contains() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               // Current
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
               // Previous
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s{type contains 'a'})[1m] <= 50%%[-1m]", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .appName("bithon")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is <= 50%, triggered
        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider,
                                                          null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_NotContains() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               // Current
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
               // Previous
               .thenReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s{type not contains 'a'})[1m] <= 50%%[-1m]", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .appName("bithon")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is <= 50%, triggered
        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_PartiallySatisfiesCondition() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(
                                            // Returns two series,
                                            // one is 5, which satisfies the condition
                                            // The other is 4, which does NOT satisfy the condition
                                            ImmutableMap.of("appName", "bithon-local", metric, 5),
                                            ImmutableMap.of("appName", "bithon-local-2", metric, 4)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s) by (appName) > 4", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .id("1")
                                       .name("test")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_NoneSatisfiesCondition() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(
                                            // Returns two series,
                                            // one is 5, which satisfies the condition
                                            // The other is 4, which does NOT satisfy the condition
                                            ImmutableMap.of("appName", "bithon-local", metric, 3),
                                            ImmutableMap.of("appName", "bithon-local-2", metric, 4)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s) by (appName) > 4", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .id("1")
                                       .name("test")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);
        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertFalse(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_AllSatisfiesCondition() throws IOException {
        Mockito.when(dataSourceProvider.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(
                                            ImmutableMap.of("appName", "bithon-local", metric, 5),
                                            ImmutableMap.of("appName", "bithon-local-2", metric, 6)))
                                        .build());

        String expr = StringUtils.format("sum(test-metrics.%s) by (appName) > 4", metric);
        AlertRule alertRule = AlertRule.builder()
                                       .id("1")
                                       .name("test")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        EvaluationContext context = new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                          CONSOLE_LOGGER,
                                                          alertRule,
                                                          dataSourceProvider, null,
                                                          null);

        new ExpressionEvaluationStep().evaluate(context);

        Assert.assertTrue(context.isExpressionEvaluatedAsTrue());
        Mockito.verify(dataSourceProvider, Mockito.times(1))
               .groupByV3(Mockito.any());
    }
}
