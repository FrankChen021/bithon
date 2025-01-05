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

package org.bithon.server.alerting.evaluator.evaluation;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.alerting.evaluator.evaluator.AlertExpressionEvaluator;
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
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
public class AlertRuleEvaluatorTest {

    private static final IEvaluationLogWriter CONSOLE_LOGGER = new IEvaluationLogWriter() {
        @Override
        public void close() {
        }

        @Override
        public void setInstance(String instance) {

        }

        @Override
        public void write(EvaluationLogEvent logEvent) {
            log.info("[{}] {}", logEvent.getClazz(), logEvent.getMessage());
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
        dataSourceProvider = EasyMock.createMock(IDataSourceApi.class);
        EasyMock.expect(dataSourceProvider.getSchemaByName(schema.getName())).andReturn(schema).anyTimes();
    }

    @Test
    public void test_GreaterThan() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LogicalAnd_AllConditionsSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
                .times(2);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 4 AND sum(test-metrics.%s)[1m] > 3", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LogicalAnd_AllConditionsNOTSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
                .times(2);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 6 AND sum(test-metrics.%s)[1m] > 6", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertFalse(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                   CONSOLE_LOGGER,
                                                                                                   alertRule,
                                                                                                   dataSourceProvider,
                                                                                                   null)));
    }

    @Test
    public void test_LogicalAnd_FirstConditionsNOTSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
                .times(1);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 6 AND sum(test-metrics.%s)[1m] > 3", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertFalse(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                   CONSOLE_LOGGER,
                                                                                                   alertRule,
                                                                                                   dataSourceProvider,
                                                                                                   null)));
    }

    @Test
    public void test_LogicalAnd_2ndConditionsNOTSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build())
                .times(2);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 3 AND sum(test-metrics.%s)[1m] > 6", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertFalse(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                   CONSOLE_LOGGER,
                                                                                                   alertRule,
                                                                                                   dataSourceProvider,
                                                                                                   null)));
    }

    @Test
    public void test_LogicalOr_AllConditionSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build())
                // Since the 1st condition satisfies, the 2nd condition should not be evaluated
                .times(1);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 2 OR sum(test-metrics.%s)[1m] = 3", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LogicalOr_1stConditionSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build())
                .times(1);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 2 OR sum(test-metrics.%s)[1m] = 3", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LogicalOr_2ndConditionSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build())
                .times(2);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 5 OR sum(test-metrics.%s)[1m] = 3", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LogicalOr_AllConditionNOTSatisfy() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build())
                .times(2);
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 5 OR sum(test-metrics.%s)[1m] = 2", metric, metric);
        IExpression expression = AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertFalse(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                   CONSOLE_LOGGER,
                                                                                                   alertRule,
                                                                                                   dataSourceProvider,
                                                                                                   null)));
    }

    @Test
    public void test_GreaterThanOrEqual() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] >= 5", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LessThan() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] < 6", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_LessThanOrEqual() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] <= 5", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }


    @Test
    public void test_IsNull_OnEmptyMap() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.emptyList())
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] is null", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void test_IsNull_OnNullReturn() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(null)
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] is null", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void testRelativeComparison_GTE() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());

        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] >= 100%%[-1m]", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void testRelativeComparison_GT() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());

        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 99%%[-1m]", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }

    @Test
    public void testRelativeComparison_LT() throws IOException {
        // Current
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        // Previous
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] < -50%%[-1m]", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is < 50%, Not triggered
        Assert.assertFalse(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                   CONSOLE_LOGGER,
                                                                                                   alertRule,
                                                                                                   dataSourceProvider,
                                                                                                   null)));
    }

    @Test
    public void testRelativeComparison_LTE() throws IOException {
        // Current
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        // Previous
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] <= 50%%[-1m]", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is <= 50%, triggered
        Assert.assertTrue(new AlertExpressionEvaluator(expression)
                              .evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                              CONSOLE_LOGGER,
                                                              alertRule,
                                                              dataSourceProvider, null)));
    }

    @Test
    public void test_Contains() throws IOException {
        // Current
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        // Previous
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s{type contains 'a'})[1m] <= 50%%[-1m]", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .appName("bithon")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is <= 50%, triggered
        Assert.assertTrue(new AlertExpressionEvaluator(expression)
                              .evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                              CONSOLE_LOGGER,
                                                              alertRule,
                                                              dataSourceProvider, null)));
    }

    @Test
    public void test_NotContains() throws IOException {
        // Current
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        // Previous
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 10)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s{type not contains 'a'})[1m] <= 50%%[-1m]", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .appName("bithon")
                                       .expr(expr)
                                       .build()
                                       .initialize();

        // Decreased by 50%, the threshold is <= 50%, triggered
        Assert.assertTrue(new AlertExpressionEvaluator(expression)
                              .evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                              CONSOLE_LOGGER,
                                                              alertRule,
                                                              dataSourceProvider,
                                                              null)));
    }

    @Test
    public void test_GroupBy_GreaterThan() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(QueryResponse.builder()
                                        .data(Collections.singletonList(ImmutableMap.of(
                                            "appName", "bithon-local",
                                            metric, 5)))
                                        .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s) by (appName) > 4", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertTrue(new AlertExpressionEvaluator(expression).evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                                                  CONSOLE_LOGGER,
                                                                                                  alertRule,
                                                                                                  dataSourceProvider,
                                                                                                  null)));
    }
}
