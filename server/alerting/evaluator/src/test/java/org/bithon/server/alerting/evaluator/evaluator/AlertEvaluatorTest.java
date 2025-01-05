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

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.alerting.evaluator.storage.local.AlertStateLocalMemoryStorage;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.AlertingStorageConfiguration;
import org.bithon.server.storage.alerting.IAlertRecordStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;
import org.bithon.server.storage.alerting.pojo.AlertStateObject;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.EvaluationLogEvent;
import org.bithon.server.storage.alerting.pojo.NotificationProps;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.alerting.AlertObjectJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.AlertRecordJdbcStorage;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.h2.H2StorageModuleAutoConfiguration;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
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
 * State test cases for {@link org.bithon.server.alerting.evaluator.evaluator.AlertEvaluator}
 *
 * @author Frank Chen
 * @date 05/1/25 4:42 PM
 */
@Slf4j
public class AlertEvaluatorTest {

    private static final IEvaluationLogWriter evaluationLogWriterStub = new IEvaluationLogWriter() {
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

    private IDataSourceApi dataSourceApiStub;

    private static INotificationApiInvoker notificationApiStub;
    private static IAlertRecordStorage alertRecordStorageStub;
    private static AlertObjectJdbcStorage alertObjectStorageStub;
    private AlertEvaluator evaluator;

    @BeforeClass
    public static void setUpStorage() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new H2StorageModuleAutoConfiguration().h2StorageModel());

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
    }

    @Before
    public void setUp() {
        notificationApiStub = Mockito.mock(INotificationApiInvoker.class);

        ISchema schema = new DefaultSchema("test-metrics",
                                           "test-metrics",
                                           new TimestampSpec("timestamp"),
                                           Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                           Collections.singletonList(new AggregateLongSumColumn(metric, metric)));
        dataSourceApiStub = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApiStub.getSchemaByName(schema.getName()))
               .thenReturn(schema);

        ServerProperties serverProperties = new ServerProperties();
        serverProperties.setPort(9897);
        evaluator = new AlertEvaluator(new AlertRepository(alertObjectStorageStub),
                                       new AlertStateLocalMemoryStorage(),
                                       evaluationLogWriterStub,
                                       alertRecordStorageStub,
                                       dataSourceApiStub,
                                       serverProperties,
                                       notificationApiStub,
                                       new ObjectMapper().configure(MapperFeature.USE_ANNOTATIONS, true));
    }

    @Test
    public void test_ReadyToReady() throws IOException {
        Mockito.when(dataSourceApiStub.groupBy(Mockito.any()))
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
                                       .expr(StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric))
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertStateObject stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.READY, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupBy(Mockito.any());
    }

    @Test
    public void test_ReadToPending() throws IOException {
        Mockito.when(dataSourceApiStub.groupBy(Mockito.any()))
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
                                       .expr(StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric))
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertStateObject stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupBy(Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_ReadToAlerting() {
        Mockito.when(dataSourceApiStub.groupBy(Mockito.any()))
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
                                       .expr(StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric))
                                       .notificationProps(NotificationProps.builder()
                                                                           .channels(List.of("console"))
                                                                           .silence(HumanReadableDuration.DURATION_30_MINUTE)
                                                                           .build())
                                       .build()
                                       .initialize();

        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           null);

        AlertStateObject stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // Check if the notification api is invoked
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());

        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupBy(Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_PendingToAlerting() {
        Mockito.when(dataSourceApiStub.groupBy(Mockito.any()))
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
                                       .expr(StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric))
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

        AlertStateObject stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.PENDING, stateObject.getStatus());

        // Check if the notification api is NOT invoked
        Mockito.verify(dataSourceApiStub, Mockito.times(1))
               .groupBy(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(0))
               .notify(Mockito.any(), Mockito.any());

        //
        // 2nd round evaluation, ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());

        // 2 times of invocation in total
        Mockito.verify(dataSourceApiStub, Mockito.times(2))
               .groupBy(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(1))
               .notify(Mockito.any(), Mockito.any());
    }

    @SneakyThrows
    @Test
    public void test_AlertingToResolved() {
        Mockito.when(dataSourceApiStub.groupBy(Mockito.any()))
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
                                        .build())
        ;

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertRule alertRule = AlertRule.builder()
                                       .id(id)
                                       .name("test-rule-1")
                                       .enabled(true)
                                       .every(HumanReadableDuration.DURATION_1_MINUTE)
                                       .forTimes(1)
                                       .expr(StringUtils.format("sum(test-metrics.%s)[1m] > 4", metric))
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

        AlertStateObject stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(1)).groupBy(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(1)).notify(Mockito.any(), Mockito.any());

        //
        // 2nd round evaluation, RESOLVED status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(2)).groupBy(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(2)).notify(Mockito.any(), Mockito.any());

        //
        // 3rd round evaluation, RESOLVED -> RESOLVED status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.RESOLVED, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(3)).groupBy(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(2)).notify(Mockito.any(), Mockito.any());

        //
        // 4th round evaluation, RESOLVED -> ALERTING status
        evaluator.evaluate(TimeSpan.now().floor(Duration.ofMinutes(1)),
                           alertRule,
                           stateObject,
                           true);
        stateObject = alertObjectStorageStub.getAlertStates().get(id);
        Assert.assertNotNull(stateObject);
        Assert.assertEquals(AlertStatus.ALERTING, stateObject.getStatus());
        Mockito.verify(dataSourceApiStub, Mockito.times(4)).groupBy(Mockito.any());
        Mockito.verify(notificationApiStub, Mockito.times(3)).notify(Mockito.any(), Mockito.any());
    }
}
