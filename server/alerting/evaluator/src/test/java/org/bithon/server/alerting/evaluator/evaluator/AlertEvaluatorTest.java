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

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.EvaluationLogger;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.serializer.AlertExpressionDeserializer;
import org.bithon.server.alerting.common.serializer.AlertExpressionSerializer;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.notification.api.INotificationApiStub;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.commons.serializer.HumanReadableDurationDeserializer;
import org.bithon.server.commons.serializer.HumanReadableDurationSerializer;
import org.bithon.server.commons.serializer.HumanReadablePercentageDeserializer;
import org.bithon.server.commons.serializer.HumanReadablePercentageSerializer;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.datasource.reader.clickhouse.ClickHouseDataSourceAutoConfiguration;
import org.bithon.server.datasource.reader.h2.H2DataSourceAutoConfiguration;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IAlertStateStorage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.NotificationProps;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageAutoConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.h2.H2StorageModuleAutoConfiguration;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.ServerProperties;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static INotificationApiStub notificationApiStub;
    private static IAlertRecordStorage alertRecordStorageStub;
    private static IAlertStateStorage alertStateStorageStub;
    private static IAlertObjectStorage alertObjectStorageStub;
    private static INotificationApiStub notificationImpl;
    private AlertEvaluator evaluator;

    @BeforeAll
    public static void setUpStorage() throws Exception {
        Map<String, Object> injection = new HashMap<>();

        ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new H2StorageModuleAutoConfiguration().h2StorageModel())
                    .registerModule(new H2DataSourceAutoConfiguration().h2DataSourceModule())
                    .registerModule(new ClickHouseStorageAutoConfiguration().clickHouseStorageModule())
                    .registerModule(new ClickHouseDataSourceAutoConfiguration().clickHouseDataSourceModule())
                    // They're configured via ObjectMapperConfigurer for production
                    .registerModule(new SimpleModule().addSerializer(AlertExpression.class, new AlertExpressionSerializer())
                                                      .addSerializer(HumanReadableDuration.class, new HumanReadableDurationSerializer())
                                                      .addSerializer(HumanReadablePercentage.class, new HumanReadablePercentageSerializer())
                                                      .addDeserializer(AlertExpression.class, new AlertExpressionDeserializer())
                                                      .addDeserializer(HumanReadableDuration.class, new HumanReadableDurationDeserializer())
                                                      .addDeserializer(HumanReadablePercentage.class, new HumanReadablePercentageDeserializer()))
                    .setInjectableValues(new InjectableValues() {
                        @Override
                        public Object findInjectableValue(Object valueId,
                                                          DeserializationContext ctx,
                                                          BeanProperty forProperty,
                                                          Object beanInstance) {
                            Class<?> targetClass = forProperty.getType().getRawClass();
                            return injection.get(targetClass.getName());
                        }
                    });

        SqlDialectManager sqlDialectManager = new SqlDialectManager(objectMapper);

        JdbcStorageProviderConfiguration h2StorageProviderConfiguration = new JdbcStorageProviderConfiguration(
            ImmutableMap.of("type", "h2",
                            "url", "jdbc:h2:mem:bithon-alerting-test;DB_CLOSE_DELAY=-1;MODE=mysql;",
                            "username", "sa"),
            sqlDialectManager
        );
        injection.put(JdbcStorageProviderConfiguration.class.getName(), h2StorageProviderConfiguration);

        injection.put(AlertingStorageConfiguration.AlertStorageConfig.class.getName(), new AlertingStorageConfiguration.AlertStorageConfig());
        injection.put(ObjectMapper.class.getName(), objectMapper);
        injection.put(SqlDialectManager.class.getName(), sqlDialectManager);

        String database = "h2"; // can be clickhouse when debugging at local
        if ("clickhouse".equals(database)) {
            ClickHouseStorageProviderConfiguration clickhouseStorageProviderConfiguration = new ClickHouseStorageProviderConfiguration(
                ImmutableMap.of("type", "clickhouse",
                                "url", "jdbc:clickhouse://localhost:8123/bithon"),
                objectMapper
            );
            injection.put(ClickHouseStorageProviderConfiguration.class.getName(), clickhouseStorageProviderConfiguration);
        }

        String type = StringUtils.format("{\"type\":\"%s\"}", database);
        alertRecordStorageStub = objectMapper.readValue(type, IAlertRecordStorage.class);
        alertRecordStorageStub.initialize();

        alertObjectStorageStub = objectMapper.readValue(type, IAlertObjectStorage.class);
        alertObjectStorageStub.initialize();

        alertStateStorageStub = objectMapper.readValue(type, IAlertStateStorage.class);
        alertStateStorageStub.initialize();

        evaluationLogWriterStub = Mockito.mock(IEvaluationLogWriter.class);

        notificationImpl = (name, message) -> {
            // Serialize and deserialize the message to simulate the remote API call
            String serialized = objectMapper.writeValueAsString(message);
            objectMapper.readValue(serialized, NotificationMessage.class);

            notificationApiStub.notify(name, message);
        };
    }

    @BeforeEach
    public void setUp() {
        notificationApiStub = Mockito.mock(INotificationApiStub.class);

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

        IEvaluationLogStorage logStorage = Mockito.mock(IEvaluationLogStorage.class);
        Mockito.when(logStorage.createWriter())
               .thenReturn(evaluationLogWriterStub);

        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(9897);
        evaluator = new AlertEvaluator(new AlertRepository(alertObjectStorageStub, alertStateStorageStub),
                                       logStorage,
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.READY, stateObject.getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.READY, stateObject.getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_ReadyToPending_GroupBy() throws IOException {
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

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
        Assertions.assertNotNull(alertState);
        Assertions.assertEquals(AlertStatus.ALERTING, alertState.getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // 2 times of invocation in total
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());
    }

    /**
     * 1. READY -> ALERTING
     * 2. ALERTING -> RESOLVED
     * 3. RESOLVED -> RESOLVED
     * 4. RESOLVED -> ALERTING
     */
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(3)).groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(2)).notify(Mockito.any(), Mockito.any());

        //
        // 4th round evaluation, RESOLVED -> ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(4)).groupByV3(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(3)).notify(Mockito.any(), Mockito.any());
    }

    @Test
    public void test_GroupBy_0_Series_Matches() throws Exception {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        // Return a value that DOES satisfy the condition,
                                        .data(Arrays.asList(ImmutableMap.of("appName", "test-app-2", metric, 88),
                                                            ImmutableMap.of("appName", "test-app-1", metric, 99)))
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 999", schema1.getName(), metric))
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.READY, stateObject.getStatus());
        Assertions.assertTrue(stateObject.getPayload().getSeries().isEmpty());
    }

    /**
     * round 1
     * series 1: READY -> PENDING
     * series 2: READY -> PENDING
     * round 2
     * series 1: PENDING -> RESOLVED
     * series 2: PENDING -> ALERTING
     */
    @Test
    public void test_GroupBy_1_SeriesMatches_PendingToAlerting() throws Exception {
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        // Check the series status
        Assertions.assertEquals(2, stateObject.getPayload().getSeries().size());
        // The status is PENDING since the rule requires 2 times of match
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check the series status
        Assertions.assertEquals(2, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());
        Assertions.assertEquals(AlertStatus.RESOLVED, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());

        // 2 times of invocation in total
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());

        // Verify notification message
        {
            ArgumentCaptor<NotificationMessage> notificationMessageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
            Mockito.verify(notificationApiStub, Mockito.times(1))
                   .notify(Mockito.any(), notificationMessageCaptor.capture());
            NotificationMessage notificationMessage = notificationMessageCaptor.getValue();
            // Only one series will be notified
            Assertions.assertEquals(AlertStatus.ALERTING, notificationMessage.getStatus());

            // ALERTING message has 1 output
            Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().size());
        }
    }

    @Test
    public void test_GroupBy_From_Multiple_DataSource() throws Exception {
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check the series status, even 3 series in two datasource above meet either one of conditions,
        // but only 1 series meet both conditions
        Assertions.assertEquals(1, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(3))
               .groupByV3(Mockito.any());

        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());

        ArgumentCaptor<NotificationMessage> notificationMessageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), notificationMessageCaptor.capture());
        NotificationMessage notificationMessage = notificationMessageCaptor.getValue();

        // There are 3 outputs from 3 sub expressions, and 1 output for each sub expression '1', '2' and '3' respectively
        Assertions.assertEquals(3, notificationMessage.getEvaluationOutputs().size());
        Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().get("1").size());
        Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().get("2").size());
        Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().get("3").size());
    }

    @Test
    public void test_ReadyToReady_MultipleExpression_No_Intersection_Across_GroupBy() throws IOException {
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.READY, stateObject.getStatus());

        // Check the series status, even 4 series in two datasource above meet either one of conditions,
        // but none of them meets ALL conditions
        Assertions.assertEquals(0, stateObject.getPayload().getSeries().size());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_AND_NonGroupBy_True() throws IOException {
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
                                           .data(List.of(ImmutableMap.of("appName", "test-app-2", metric, 88)))
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
                                                                + "AND sum(%s.%s)[1m] > 5 ",
                                                                schema1.getName(), metric,
                                                                schema2.getName(), metric))
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check the series status, even 3 series in two datasource above meet either one of conditions,
        // but only 1 series meet both conditions
        Assertions.assertEquals(3, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.EMPTY).getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_NonGroupBy_AND_GroupBy_False() throws IOException {
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
                   throw new RuntimeException("Unknown data source");
               });

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4 "
                                                                // no match with GROUP-BY
                                                                + "AND sum(%s.%s)[1m] by (appName) > 999",
                                                                schema1.getName(), metric,
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
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.READY, stateObject.getStatus());
        Assertions.assertEquals(0, stateObject.getPayload().getSeries().size());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_RelativeComparison_NoMatch() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 100),
                                                            ImmutableMap.of("appName", "test-app-2", metric, 200)))
                                        .build()
               )
               // Base line
               .thenReturn(
                   QueryResponse.builder()
                                // Return a value that DOES satisfy the condition,
                                .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 100),
                                                    ImmutableMap.of("appName", "test-app-2", metric, 200)))
                                .build()
               );

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 99%%[-1m] ",
                                                                schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.READY, stateObject.getStatus());
        Assertions.assertEquals(0, stateObject.getPayload().getSeries().size());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_RelativeComparison_1_Series_Match() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 120),
                                                            ImmutableMap.of("appName", "test-app-2", metric, 200)))
                                        .build()
               )
               // Base line
               .thenReturn(
                   QueryResponse.builder()
                                .data(Arrays.asList(
                                    // This one matches the condition
                                    ImmutableMap.of("appName", "test-app-1", metric, 50),
                                    // This one does NOT match
                                    ImmutableMap.of("appName", "test-app-2", metric, 200)))
                                .build()
               );

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 100%%[-1m] ",
                                                                schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Assertions.assertEquals(1, stateObject.getPayload().getSeries().size());

        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    @Test
    public void test_GroupBy_RelativeComparison_ALL_Series_Match() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenReturn(QueryResponse.builder()
                                        .data(Arrays.asList(ImmutableMap.of("appName", "test-app-1", metric, 120),
                                                            ImmutableMap.of("appName", "test-app-2", metric, 200)))
                                        .build()
               )
               // Base line
               .thenReturn(
                   QueryResponse.builder()
                                .data(Arrays.asList(
                                    // This one matches the condition
                                    ImmutableMap.of("appName", "test-app'-1", metric, 50),
                                    // This one DOES match
                                    ImmutableMap.of("appName", "test-app'-2", metric, 30)))
                                .build()
               );

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 100%%[-1m] ",
                                                                schema1.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Assertions.assertEquals(2, stateObject.getPayload().getSeries().size());

        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupByV3(Mockito.any());
    }

    /**
     * To test that if SAME series appear in multiple sub-expressions, the state(successive match count) are calculated correctly
     */
    @Test
    public void test_SameLabel_MatchCountGreaterThan_One() throws IOException {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               .thenAnswer((invocation) -> {
                   QueryRequest queryRequest = invocation.getArgument(0);
                   if (queryRequest.getDataSource().equals(schema1.getName())) {
                       return QueryResponse.builder()
                                           // Return a value that DOES satisfy the condition,
                                           .data(List.of(ImmutableMap.of("appName", "test-app-1", metric, 88)))
                                           .build();
                   }

                   if (queryRequest.getDataSource().equals(schema2.getName())) {
                       return QueryResponse.builder()
                                           // Return a value that DOES satisfy the condition,
                                           .data(List.of(ImmutableMap.of("appName", "test-app-2", metric, 100)))
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
                                       //
                                       // Two sub expression, expected the whole expression are evaluated as true for 2 times
                                       //
                                       .forTimes(2)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] > 4 "
                                                                // no match with GROUP-BY
                                                                + "AND sum(%s.%s)[1m] > 99",
                                                                schema1.getName(), metric,
                                                                schema2.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 0
                                                                           .silence(HumanReadableDuration.of(0, TimeUnit.SECONDS))
                                                                           .build())
                                       .build()
                                       .initialize();

        // First round evaluation, expect PENDING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getStatus());
        Assertions.assertEquals(1, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.PENDING, stateObject.getPayload().getSeries().get(Label.EMPTY).getStatus());

        // 2nd round evaluation, expect ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true /*Skip interval check for test case*/);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Assertions.assertEquals(1, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.EMPTY).getStatus());
        Assertions.assertNotEquals(0L, Timestamp.valueOf(stateObject.getLastAlertAt()).getTime());
        Assertions.assertNotNull(stateObject.getLastRecordId());

        Mockito.verify(dataSourceApiStub, Mockito.times(4))
               .groupByV3(Mockito.any());
    }

    /**
     * 1. series 1, 2 --> {NON, ALERTING}
     * 2. series 1, 2 --> {ALERTING, SUPPRESSING}
     * 3. series 1, 2 --> {SUPPRESSING, SUPPRESSING}
     * 4. series 1, 2 --> {RESOLVED, SUPPRESSING}
     * 5. series 1, 2 --> {RESOLVED, RESOLVED}
     */
    @Test
    public void test_MultipleLabels_One_Alerting_After_Another() throws Exception {
        Mockito.when(dataSourceApiStub.groupByV3(Mockito.any()))
               // 1
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(
                                            ImmutableMap.of("appName", "test-app-1", metric, 50),   // Not Satisfy
                                            ImmutableMap.of("appName", "test-app-2", metric, 100))) // Satisfy --> ALERTING
                                        .build())
               // 2
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(
                                            ImmutableMap.of("appName", "test-app-1", metric, 100), // Satisfy --> ALERTING
                                            ImmutableMap.of("appName", "test-app-2", metric, 100))) // Satisfy --> SUPPRESSING
                                        .build())
               // 3
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(
                                            ImmutableMap.of("appName", "test-app-1", metric, 100), // Satisfy --> SUPPRESSING
                                            ImmutableMap.of("appName", "test-app-2", metric, 100)))// Satisfy --> SUPPRESSING
                                        .build())
               // 4
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(
                                            ImmutableMap.of("appName", "test-app-1", metric, 5), // NOT Satisfy --> RESOLVED
                                            ImmutableMap.of("appName", "test-app-2", metric, 100)))// Satisfy --> SUPPRESSING
                                        .build())
               // 5
               .thenReturn(QueryResponse.builder()
                                        .data(List.of(
                                            ImmutableMap.of("appName", "test-app-1", metric, 5), // NOT Satisfy --> RESOLVED
                                            ImmutableMap.of("appName", "test-app-2", metric, 5)))// NOT Satisfy --> RESOLVED
                                        .build());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       // Two sub expression, expected the whole expression are evaluated as true for 2 times
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(%s.%s)[1m] by (appName) > 57",
                                                                schema1.getName(), metric,
                                                                schema2.getName(), metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           // Silence is 1min
                                                                           .silence(HumanReadableDuration.of(1, TimeUnit.MINUTES))
                                                                           .build())
                                       .build()
                                       .initialize();

        //
        // First round evaluation, series 2 will be in ALERTING
        //
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);
        AlertState stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Assertions.assertEquals(1, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Verify notification message
        {
            ArgumentCaptor<NotificationMessage> notificationMessageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
            Mockito.verify(notificationApiStub, Mockito.times(1))
                   .notify(Mockito.any(), notificationMessageCaptor.capture());
            NotificationMessage notificationMessage = notificationMessageCaptor.getValue();
            Assertions.assertNotNull(notificationMessage.getEvaluationOutputs().get("1"));
            // Only one series will be notified
            Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().get("1").size());
        }

        //
        // 2nd round, series 1 will be ALERTING
        //
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Assertions.assertEquals(2, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.ALERTING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assertions.assertEquals(AlertStatus.SUPPRESSING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Verify notification message
        {
            ArgumentCaptor<NotificationMessage> notificationMessageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);

            Mockito.verify(notificationApiStub, Mockito.times(2))
                   .notify(Mockito.any(), notificationMessageCaptor.capture());
            NotificationMessage notificationMessage = notificationMessageCaptor.getValue();
            Assertions.assertNotNull(notificationMessage.getEvaluationOutputs().get("1"));
            // Only one series will be notified
            Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().get("1").size());
            Assertions.assertEquals(Label.builder().add("appName", "test-app-1").build(), notificationMessage.getEvaluationOutputs().get("1").get(0).getLabel());
        }

        // 3rd round, both are under SUPPRESSING
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.SUPPRESSING, stateObject.getStatus());
        Assertions.assertEquals(2, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.SUPPRESSING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assertions.assertEquals(AlertStatus.SUPPRESSING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());
        // Verify notification message
        // in this case, no notification sent, the expected call is still 2
        Mockito.verify(notificationApiStub, Mockito.times(2))
               .notify(Mockito.any(), Mockito.any());

        //
        // 4th round, series 1 will be RESOLVED
        //
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.SUPPRESSING, stateObject.getStatus());
        // because series 1 is RESOLVED, ONLY series-2 will be kept
        Assertions.assertEquals(2, stateObject.getPayload().getSeries().size());
        Assertions.assertEquals(AlertStatus.RESOLVED, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-1").build()).getStatus());
        Assertions.assertEquals(AlertStatus.SUPPRESSING, stateObject.getPayload().getSeries().get(Label.builder().add("appName", "test-app-2").build()).getStatus());

        // Verify notification message
        {
            ArgumentCaptor<NotificationMessage> notificationMessageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
            Mockito.verify(notificationApiStub, Mockito.times(3))
                   .notify(Mockito.any(), notificationMessageCaptor.capture());
            NotificationMessage notificationMessage = notificationMessageCaptor.getValue();
            // Only one series will be notified
            Assertions.assertEquals(AlertStatus.RESOLVED, notificationMessage.getStatus());

            // RESOLVED message has 1 output
            Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().size());
            Assertions.assertEquals(Map.of("appName", "test-app-1"), notificationMessage.getEvaluationOutputs().get("").get(0).getLabel().getKeyValues());
        }

        //
        // 5th round, both are RESOLVED, but only series 2 will be notified
        //
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertStateStorageStub.getAlertStates().get(id);
        Assertions.assertNotNull(stateObject);
        Assertions.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
        // Verify notification message
        {
            ArgumentCaptor<NotificationMessage> notificationMessageCaptor = ArgumentCaptor.forClass(NotificationMessage.class);
            Mockito.verify(notificationApiStub, Mockito.times(4))
                   .notify(Mockito.any(), notificationMessageCaptor.capture());
            NotificationMessage notificationMessage = notificationMessageCaptor.getValue();
            // Only one series will be notified
            Assertions.assertEquals(AlertStatus.RESOLVED, notificationMessage.getStatus());

            // RESOLVED message has 1 output
            Assertions.assertEquals(1, notificationMessage.getEvaluationOutputs().size());
            Assertions.assertEquals(Map.of("appName", "test-app-2"), notificationMessage.getEvaluationOutputs().get("").get(0).getLabel().getKeyValues());
        }

        Mockito.verify(dataSourceApiStub, Mockito.times(5))
               .groupByV3(Mockito.any());
    }
}
