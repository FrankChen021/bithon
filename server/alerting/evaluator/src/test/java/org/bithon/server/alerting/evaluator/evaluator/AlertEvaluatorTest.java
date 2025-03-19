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

package org.bithon.server.alerting.evaluator.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationLogger;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.serializer.AlertExpressionDeserializer;
import org.bithon.server.alerting.common.serializer.AlertExpressionSerializer;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.state.local.LocalStateManager;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.commons.serializer.HumanReadableDurationDeserializer;
import org.bithon.server.commons.serializer.HumanReadableDurationSerializer;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.NotificationProps;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.alerting.AlertObjectJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.AlertRecordJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.AlertStateJdbcStorage;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.h2.H2StorageModuleAutoConfiguration;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * State test cases for {@link AlertEvaluator}
 *
 * @author Frank Chen
 * @date 05/1/25 4:42 PM
 */
@Slf4j
public class AlertEvaluatorTest {

    private final String metric = "count";

    private IDataSourceApi dataSourceApiStub;

    private static ISchema schema1;
    private static ISchema schema2;
    private static ISchema schema3;
    private static IEvaluationLogWriter evaluationLogWriterStub;
    private static INotificationApiInvoker notificationApiStub;
    private static IAlertRecordStorage alertRecordStorageStub;
    private static AlertStateJdbcStorage alertStateStorageStub;
    private static AlertObjectJdbcStorage alertObjectStorageStub;
    private static INotificationApiInvoker notificationImpl;
    private AlertEvaluator evaluator;

    @BeforeClass
    public static void setUpStorage() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new H2StorageModuleAutoConfiguration().h2StorageModel())
                    // They're configured via ObjectMapperConfigurer for production
                    .registerModule(new SimpleModule().addSerializer(AlertExpression.class, new AlertExpressionSerializer())
                                                      .addSerializer(HumanReadableDuration.class, new HumanReadableDurationSerializer())
                                                      .addDeserializer(AlertExpression.class, new AlertExpressionDeserializer())
                                                      .addDeserializer(HumanReadableDuration.class, new HumanReadableDurationDeserializer()));

        SqlDialectManager sqlDialectManager = new SqlDialectManager(objectMapper);

        JdbcStorageProviderConfiguration jdbcStorageProviderConfiguration = new JdbcStorageProviderConfiguration(
            ImmutableMap.of("type", "h2",
                            "url", "jdbc:h2:mem:bithon-alerting-test;DB_CLOSE_DELAY=-1;MODE=mysql;",
                            "username", "sa"),
            sqlDialectManager
        );
        alertRecordStorageStub = new AlertRecordJdbcStorage(jdbcStorageProviderConfiguration,
                                                            new AlertingStorageConfiguration.AlertStorageConfig(),
                                                            objectMapper);
        alertRecordStorageStub.initialize();

        alertObjectStorageStub = new AlertObjectJdbcStorage(jdbcStorageProviderConfiguration,
                                                            sqlDialectManager,
                                                            objectMapper,
                                                            new AlertingStorageConfiguration.AlertStorageConfig());
        alertObjectStorageStub.initialize();

        alertStateStorageStub = new AlertStateJdbcStorage(jdbcStorageProviderConfiguration,
                                                          new AlertingStorageConfiguration.AlertStorageConfig(),
                                                          sqlDialectManager,
                                                          objectMapper);
        alertStateStorageStub.initialize();

        evaluationLogWriterStub = Mockito.mock(IEvaluationLogWriter.class);

        notificationImpl = (name, message) -> {
            // Serialize and deserialize the message to simulate the remote API call
            String serialized = objectMapper.writeValueAsString(message);
            objectMapper.readValue(serialized, NotificationMessage.class);

            notificationApiStub.notify(name, message);
        };
    }

    @Before
    public void setUp() {
        notificationApiStub = Mockito.mock(INotificationApiInvoker.class);

        schema1 = new DefaultSchema("test-metrics",
                                    "test-metrics",
                                    new TimestampSpec("timestamp"),
                                    Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                    Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        dataSourceApiStub = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApiStub.getSchemaByName(schema1.getName()))
               .thenReturn(schema1);

        schema2 = new DefaultSchema("test-metrics-2",
                                    "test-metrics-2",
                                    new TimestampSpec("timestamp"),
                                    Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                    Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        Mockito.when(dataSourceApiStub.getSchemaByName(schema2.getName()))
               .thenReturn(schema2);

        schema3 = new DefaultSchema("test-metrics-3",
                                    "test-metrics-3",
                                    new TimestampSpec("timestamp"),
                                    Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                    Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        Mockito.when(dataSourceApiStub.getSchemaByName(schema3.getName()))
               .thenReturn(schema3);

        ISchema schema2 = new DefaultSchema("test-metrics-2",
                                            "test-metrics-2",
                                            new TimestampSpec("timestamp"),
                                            Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                            Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        Mockito.when(dataSourceApiStub.getSchemaByName(schema2.getName()))
               .thenReturn(schema2);

        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(9897);
        evaluator = new AlertEvaluator(new AlertRepository(alertObjectStorageStub),
                                       new LocalStateManager(alertStateStorageStub),
                                       evaluationLogWriterStub,
                                       alertRecordStorageStub,
                                       dataSourceApiStub,
                                       serverProperties,
                                       notificationImpl,
                                       JsonMapper.builder().build());
    }

    @Test
    public void test_ReadyToReady() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that does NOT satisfy the condition,
                                        // Alert Status keeps unchanged
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4", schema1.getName(), metric))
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.READY, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_ReadyToReady_groupByV3() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that does NOT satisfy the condition,
                                        // Alert Status keeps unchanged
                                        .data(Collections.singletonList(ImmutableMap.of(
                                            "appName", "bithon-test-app",
                                            metric, 3)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4", schema1.getName(), metric))
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.READY, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_ReadyToPending() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 5)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4", schema1.getName(), metric))
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_ReadyToPending_groupByV3() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of("appName", "bithon-test-app", metric, 5)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4", schema1.getName(), metric))
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_ReadyToAlerting() {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 7)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       // Match times is 1, should go to ALERTING status
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4", schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           .silence(HumanReadableDuration.DURATION_30_MINUTE)
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check if the notification api is invoked
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_ReadyToAlerting_GroupBy_OneGroupAlerting() {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(
                                            // The first series satisfies the condition,
                                            ImmutableMap.of("appName", "test-app-1", metric, 7),
                                            // The 2nd DOES NOT satisfy the condition
                                            ImmutableMap.of("appName", "test-app-2", metric, 3)
                                        ))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       // Match times is 1, should go to ALERTING status
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4", schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           .silence(HumanReadableDuration.DURATION_30_MINUTE)
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState alertState = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(alertState);
        Assert.assertEquals(AlertStatus.ALERTING, alertState.getStatus());

        // Check if the notification api is invoked
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_ReadyToAlerting_GroupBy_AllGroupAlerting() {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 7),
                                                            ImmutableMap.of("appName", "test-app-2", metric, 8)
                                        ))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       // Match times is 1, should go to ALERTING status
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4", schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           .silence(HumanReadableDuration.DURATION_30_MINUTE)
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check if the notification api is invoked
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_PendingToAlerting() {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 88)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 99)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4", schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        //
        // First round evaluation, expect PENDING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(0))
               .notify(Mockito.any(), Mockito.any());

        //
        // 2nd round evaluation, ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // 2 times of invocation in total
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_PendingToAlerting_GroupBy_1SeriesMatches() {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Arrays.asList(ImmutableMap.of("appName", "test-app-2", metric, 88),
                                                            ImmutableMap.of("appName", "test-app-1", metric, 99)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(List.of(ImmutableMap.of("appName", "test-app-2", metric, 88)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4", schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        //
        // First round evaluation, expect PENDING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        // Check the series status
        Assert.assertEquals(2, stateObject.getPayload().getSeries().size());
        // The status is PENDING since the rule requires 2 times of match
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(0))
               .notify(Mockito.any(), Mockito.any());

        //
        // 2nd round evaluation, ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check the series status
        Assert.assertEquals(1, stateObject.getPayload().getSeries().size());
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // 2 times of invocation in total
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_AlertingToResolved() {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 100)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES NOT satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 3)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES NOT satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 2)))
                                        .build())
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Collections.singletonList(ImmutableMap.of(metric, 101)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4", schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        //
        // 1st round evaluation, expect ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        new EvaluationLogger(evaluationLogWriterStub).info(alertRule.getId(),
                                                           alertRule.getName(),
                                                           AlertEvaluatorTest.class,
                                                           "=====================Evaluating 1st==================");
        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(1)).groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(1)).notify(Mockito.any(), Mockito.any());

        //
        // 2nd round evaluation, RESOLVED status
        new EvaluationLogger(evaluationLogWriterStub).info(alertRule.getId(),
                                                           alertRule.getName(),
                                                           AlertEvaluatorTest.class,
                                                           "=====================Evaluating 2nd==================");
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(2)).groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(2)).notify(Mockito.any(), Mockito.any());

        //
        // 3rd round evaluation, RESOLVED -> RESOLVED status
        //
        new EvaluationLogger(evaluationLogWriterStub).info(alertRule.getId(),
                                                           alertRule.getName(),
                                                           AlertEvaluatorTest.class,
                                                           "=====================Evaluating 3rd==================");
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        // Verify states
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(3)).groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(2)).notify(Mockito.any(), Mockito.any());

        //
        // 4th round evaluation, RESOLVED -> ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(4)).groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(3)).notify(Mockito.any(), Mockito.any());
    }

    @Test
    public void test_PendingToAlerting_MultipleExpression_WithgroupByV3() throws IOException {
        //
        // test-app-2, which meets the alerting threshold, are in 3 data sources
        //
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenAnswer((invocation) -> {
                   QueryRequest queryRequest = invocation.getArgument(0);
                   if (queryRequest.getDataSource().equals(schema1.getName())) {
                       return QueryResponse.builder()
                                           // Return a value that DOES satisfy the condition,
                                           .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 88),
                                                               ImmutableMap.of("appName", "test-app-2", metric, 99)))
                                           .build();
                   }

                   if (queryRequest.getDataSource().equals(schema2.getName())) {
                       return QueryResponse.builder()
                                           // Return a value that DOES satisfy the condition,
                                           .data(Arrays.asList(ImmutableMap.of("appName", "test-app-2", metric, 88),
                                                               ImmutableMap.of("appName", "test-app-3", metric, 99)))
                                           .build();
                   }

                   if (queryRequest.getDataSource().equals(schema3.getName())) {
                       return QueryResponse.builder()
                                           // Return a value that DOES satisfy the condition,
                                           .data(Arrays.asList(ImmutableMap.of("appName", "test-app-2", metric, 77),
                                                               ImmutableMap.of("appName", "test-app-4", metric, 88)))
                                           .build();
                   }
                   throw new RuntimeException("Unknown data source");
               });

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4 "
                                                                + "AND sum(%s.%s)[1m] by (appName) > 5 "
                                                                + "AND sum(%s.%s)[1m] by (appName) > 7", schema1.getName(), metric,
                                                                schema2.getName(), metric,
                                                                schema3.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        //
        // First round evaluation, expect PENDING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check the series status, even 3 series in two datasource above meet either one of conditions,
        // but only 1 series meet both conditions
        Assert.assertEquals(1, stateObject.getPayload().getSeries().size());
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(3))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_ReadyToReady_MultipleExpression_No_Intersection_Across_groupByV3() throws IOException {
        //
        // test-app-2, which meets the alerting threshold, are in two data sources
        //
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenAnswer((invocation) -> {
                   QueryRequest queryRequest = invocation.getArgument(0);
                   if (queryRequest.getDataSource().equals(schema1.getName())) {
                       return QueryResponse.builder()
                                           // Return a value that DOES satisfy the condition,
                                           .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 88),
                                                               ImmutableMap.of("appName", "test-app-2", metric, 99)))
                                           .build();
                   }

                   if (queryRequest.getDataSource().equals(schema2.getName())) {
                       return QueryResponse.builder()
                                           // Return different labels from previous data source
                                           .data(Arrays.asList(ImmutableMap.of("appName", "test-app-3", metric, 88),
                                                               ImmutableMap.of("appName", "test-app-4", metric, 99)))
                                           .build();
                   }
                   throw new RuntimeException("Unknown data source");
               });

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 4 "
                                                                + "AND sum(%s.%s)[1m] by (appName) > 5",
                                                                schema1.getName(),
                                                                metric,
                                                                schema2.getName(),
                                                                metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        //
        // First round evaluation, expect PENDING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.READY, stateObject.getStatus());

        // Check the series status, even 4 series in two datasource above meet either one of conditions,
        // but none of them meets ALL conditions
        Assert.assertEquals(0, stateObject.getPayload().getSeries().size());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }
}
