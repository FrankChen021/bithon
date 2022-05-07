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

package org.bithon.server.alerting.processor.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.common.evaluator.EvaluatorContext;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.GreaterThanMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.LessOrEqualMetricCondition;
import org.bithon.server.alerting.common.evaluator.metric.absolute.NullValueMetricCondition;
import org.bithon.server.alerting.common.evaluator.rule.UnaryEvaluator;
import org.bithon.server.alerting.common.model.AggregatorEnum;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertCondition;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.IEvaluatorLogWriter;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.spec.LongSumMetricSpec;
import org.bithon.server.web.service.api.IDataSourceApi;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * @author Frank Chen
 * @date 28/3/22 9:42 PM
 */
@Slf4j
public class AlertConditionTest {

    private static final IEvaluatorLogWriter CONSOLE_LOGGER = new IEvaluatorLogWriter() {
        @Override
        public void log(String alertId, String alertName, Class<?> logClass, String message) {
            log.info("[{}] {}", logClass.getSimpleName(), message);
        }

        @Override
        public void flush() {
        }
    };

    private final String metric = "count";
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private IDataSourceApi dataSourceProvider;

    @Before
    public void setUp() {

        DataSourceSchema schema = new DataSourceSchema("test-metrics",
                                                       "test-metrics",
                                                       null,
                                                       Collections.emptyList(),
                                                       Collections.singletonList(new LongSumMetricSpec(metric, metric, metric, "", true)));
        dataSourceProvider = EasyMock.createMock(IDataSourceApi.class);
        EasyMock.expect(dataSourceProvider.getSchemaByName(schema.getName())).andReturn(schema);
    }

    @After
    public void tearDown() {
        EasyMock.verify(dataSourceProvider);
    }

    @Test
    public void testConditionGreaterThan() throws JsonProcessingException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject())).andReturn(Collections.singletonList(ImmutableMap.of(metric, 5)));
        EasyMock.replay(dataSourceProvider);

        AlertCondition condition = AlertCondition.builder()
                                                 .id("A")
                                                 .dataSource("test-metrics")
                                                 .metric(new GreaterThanMetricCondition(metric, AggregatorEnum.sum, 4, 1))
                                                 .build();

        Alert alert = Alert.builder()
                           .conditions(Collections.singletonList(objectMapper.readValue(objectMapper.writeValueAsString(condition),
                                                                                        AlertCondition.class)))
                           .build();

        UnaryEvaluator evaluator = new UnaryEvaluator(condition);
        Assert.assertTrue(evaluator.test(new EvaluatorContext(TimeSpan.nowInMinute(),
                                                              CONSOLE_LOGGER,
                                                              alert,
                                                              dataSourceProvider)));

    }

    @Test
    public void testConditionGreaterThanOrEqual() throws JsonProcessingException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject())).andReturn(Collections.singletonList(ImmutableMap.of(metric, 5)));
        EasyMock.replay(dataSourceProvider);

        AlertCondition condition = AlertCondition.builder()
                                                 .id("A")
                                                 .dataSource("test-metrics")
                                                 .metric(new GreaterOrEqualMetricCondition(metric, AggregatorEnum.sum, 5, 1))
                                                 .build();

        Alert alert = Alert.builder()
                           .conditions(Collections.singletonList(objectMapper.readValue(objectMapper.writeValueAsString(condition),
                                                                                        AlertCondition.class)))
                           .build();

        UnaryEvaluator evaluator = new UnaryEvaluator(condition);
        Assert.assertTrue(evaluator.test(new EvaluatorContext(TimeSpan.nowInMinute(),
                                                              CONSOLE_LOGGER,
                                                              alert,
                                                              dataSourceProvider)));
    }

    @Test
    public void testConditionLessThan() throws JsonProcessingException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject())).andReturn(Collections.singletonList(ImmutableMap.of(metric, 5)));
        EasyMock.replay(dataSourceProvider);

        AlertCondition condition = AlertCondition.builder()
                                                 .id("A")
                                                 .dataSource("test-metrics")
                                                 .metric(new LessOrEqualMetricCondition(metric, AggregatorEnum.sum, 6, 1))
                                                 .build();

        Alert alert = Alert.builder()
                           .conditions(Collections.singletonList(objectMapper.readValue(objectMapper.writeValueAsString(condition),
                                                                                        AlertCondition.class)))
                           .build();

        UnaryEvaluator evaluator = new UnaryEvaluator(condition);
        Assert.assertTrue(evaluator.test(new EvaluatorContext(TimeSpan.nowInMinute(),
                                                              CONSOLE_LOGGER,
                                                              alert,
                                                              dataSourceProvider)));
    }

    @Test
    public void testConditionLessThanOrEqual() throws JsonProcessingException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject())).andReturn(Collections.singletonList(ImmutableMap.of(metric, 5)));
        EasyMock.replay(dataSourceProvider);

        AlertCondition condition = AlertCondition.builder()
                                                 .id("A")
                                                 .dataSource("test-metrics")
                                                 .metric(new LessOrEqualMetricCondition(metric, AggregatorEnum.sum, 5, 1))
                                                 .build();

        Alert alert = Alert.builder()
                           .conditions(Collections.singletonList(objectMapper.readValue(objectMapper.writeValueAsString(condition),
                                                                                        AlertCondition.class)))
                           .build();

        UnaryEvaluator evaluator = new UnaryEvaluator(condition);
        Assert.assertTrue(evaluator.test(new EvaluatorContext(TimeSpan.nowInMinute(),
                                                              CONSOLE_LOGGER,
                                                              alert,
                                                              dataSourceProvider)));
    }


    @Test
    public void testConditionNull_OnEmptyMap() throws JsonProcessingException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject())).andReturn(Collections.emptyList());
        EasyMock.replay(dataSourceProvider);

        AlertCondition condition = AlertCondition.builder()
                                                 .id("A")
                                                 .dataSource("test-metrics")
                                                 .metric(new NullValueMetricCondition(metric, 1))
                                                 .build();

        Alert alert = Alert.builder()
                           .conditions(Collections.singletonList(objectMapper.readValue(objectMapper.writeValueAsString(condition),
                                                                                        AlertCondition.class)))
                           .build();

        UnaryEvaluator evaluator = new UnaryEvaluator(condition);
        Assert.assertTrue(evaluator.test(new EvaluatorContext(TimeSpan.nowInMinute(),
                                                              CONSOLE_LOGGER,
                                                              alert,
                                                              dataSourceProvider)));
    }

    @Test
    public void testConditionNull_OnNullReturn() throws JsonProcessingException {
        EasyMock.expect(dataSourceProvider.groupBy(EasyMock.anyObject())).andReturn(null);
        EasyMock.replay(dataSourceProvider);

        AlertCondition condition = AlertCondition.builder()
                                                 .id("A")
                                                 .dataSource("test-metrics")
                                                 .metric(new NullValueMetricCondition(metric, 1))
                                                 .build();

        Alert alert = Alert.builder()
                           .conditions(Collections.singletonList(objectMapper.readValue(objectMapper.writeValueAsString(condition),
                                                                                        AlertCondition.class)))
                           .build();

        UnaryEvaluator evaluator = new UnaryEvaluator(condition);
        Assert.assertTrue(evaluator.test(new EvaluatorContext(TimeSpan.nowInMinute(),
                                                              CONSOLE_LOGGER,
                                                              alert,
                                                              dataSourceProvider)));
    }
}
