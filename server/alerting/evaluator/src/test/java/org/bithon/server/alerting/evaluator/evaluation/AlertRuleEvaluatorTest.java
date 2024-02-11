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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.web.service.datasource.api.GeneralQueryResponse;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 28/3/22 9:42 PM
 */
@Slf4j
public class AlertRuleEvaluatorTest {

    private static final IEvaluationLogWriter CONSOLE_LOGGER = new IEvaluationLogWriter() {
        @Override
        public void log(String alertId, String alertName, Class<?> logClass, String message) {
            log.info("[{}] {}", logClass.getSimpleName(), message);
        }

        @Override
        public void flush() {
        }
    };

    private final String metric = "count";

    private IDataSourceApi dataSourceProvider;

    @Before
    public void setUp() {
        ISchema schema = new DefaultSchema("test-metrics",
                                           "test-metrics",
                                           new TimestampSpec("timestamp", null, null),
                                           Collections.emptyList(),
                                           Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        dataSourceProvider = EasyMock.createMock(IDataSourceApi.class);
        EasyMock.expect(dataSourceProvider.getSchemaByName(schema.getName())).andReturn(schema);
    }

    @After
    public void tearDown() {
        EasyMock.verify(dataSourceProvider);
    }

    @Test
    public void testConditionGreaterThan() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(GeneralQueryResponse.builder()
                                               .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                               .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertEquals(true, expression.evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                            CONSOLE_LOGGER,
                                                                            alertRule,
                                                                            dataSourceProvider)));
    }

    @Test
    public void testConditionGreaterThanOrEqual() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(GeneralQueryResponse.builder()
                                               .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                               .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] >= 5", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertEquals(true, expression.evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                            CONSOLE_LOGGER,
                                                                            alertRule,
                                                                            dataSourceProvider)));
    }

    @Test
    public void testConditionLessThan() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(GeneralQueryResponse.builder()
                                               .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                               .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] < 6", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertEquals(true, expression.evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                            CONSOLE_LOGGER,
                                                                            alertRule,
                                                                            dataSourceProvider)));
    }

    @Test
    public void testConditionLessThanOrEqual() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(GeneralQueryResponse.builder()
                                               .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                               .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] <= 5", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertEquals(true, expression.evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                            CONSOLE_LOGGER,
                                                                            alertRule,
                                                                            dataSourceProvider)));
    }


    @Test
    public void testConditionNull_OnEmptyMap() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(GeneralQueryResponse.builder()
                                               .data(Collections.emptyList())
                                               .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] is null", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertEquals(true, expression.evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                            CONSOLE_LOGGER,
                                                                            alertRule,
                                                                            dataSourceProvider)));
    }

    @Test
    public void testConditionNull_OnNullReturn() throws IOException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject()))
                .andReturn(GeneralQueryResponse.builder()
                                               .data(null)
                                               .build());
        EasyMock.replay(dataSourceProvider);

        String expr = StringUtils.format("sum(test-metrics.%s)[1m] is null", metric);
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse(expr);

        AlertRule alertRule = AlertRule.builder()
                                       .expr(expr)
                                       .build()
                                       .initialize();

        Assert.assertEquals(true, expression.evaluate(new EvaluationContext(TimeSpan.now().floor(Duration.ofMinutes(1)),
                                                                            CONSOLE_LOGGER,
                                                                            alertRule,
                                                                            dataSourceProvider)));
    }
}
