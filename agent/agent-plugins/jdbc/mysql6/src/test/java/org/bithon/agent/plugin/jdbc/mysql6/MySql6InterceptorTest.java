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

package org.bithon.agent.plugin.jdbc.mysql6;

import com.mysql.cj.jdbc.ConnectionImpl;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InterceptorInstaller;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.exporter.InMemoryMessageExporterFactory;
import org.bithon.agent.observability.metric.domain.sql.SQLMetricStorage;
import org.bithon.agent.observability.metric.domain.sql.SQLMetrics;
import org.bithon.agent.observability.metric.model.AbstractMetricStorage;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.metric.model.schema.Dimensions;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class MySql6InterceptorTest {

    private static final Logger logger = LoggerFactory.getLogger(MySql6InterceptorTest.class);

    private static MySQLContainer<?> mysqlServerContainer;
    protected static IMessageConverter MESSAGE_CONVERTER = new InMemoryMessageExporterFactory.RawMessageConverter();

    @BeforeAll
    static void setUpDatabase() {
        //
        // Initialize agent to install interceptors first
        //
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Dbithon.application.name=test",
                                                       "-Dbithon.application.env=local",
                                                       "-Dbithon.application.port=9897"));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(
                                 ImmutableMap.of(
                                     "bithon_exporters_tracing_client_factory", InMemoryMessageExporterFactory.class.getName(),
                                     "bithon_exporters_metric_client_factory", InMemoryMessageExporterFactory.class.getName(),
                                     "bithon_tracing_debug", "true")
                             );

            ConfigurationManager.createForTesting(new File("not-exists"));
        }

        //
        // Install interceptors
        //
        InterceptorInstaller.install(new MySql6Plugin(), MySql6InterceptorTest.class.getClassLoader());

        //
        // Use 5.7 server with stability improvements for 6.x driver
        //
        //noinspection resource
        mysqlServerContainer = new MySQLContainer<>("mysql:5.7.44")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-mysql6.sql")
            .withCommand("--default-authentication-plugin=mysql_native_password", 
                        "--sql_mode=TRADITIONAL",
                        "--skip-log-bin",
                        "--innodb-buffer-pool-size=128M",
                        "--max-connections=100",
                        "--query_cache_size=0",
                        "--query_cache_type=0")
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "testdb")
            .withEnv("MYSQL_USER", "test")
            .withEnv("MYSQL_PASSWORD", "test");

        logger.info("Starting MySQL container...");
        mysqlServerContainer.start();
    }

    @AfterAll
    static void tearDown() {
        logger.info("Stopping MySQL container");
        if (mysqlServerContainer != null) {
            mysqlServerContainer.close();
        }
    }

    @BeforeEach
    public void beforeEach() {
        SQLMetricStorage.getInstance().collect(MESSAGE_CONVERTER, 0, 0);
        InMemoryMessageExporterFactory.InMemoryMetricExporter.clear();
    }

    private String getClientSideConnectionString() {
        // Add SSL configuration and MySQL 8.0 compatibility for MySQL 6 driver
        return mysqlServerContainer.getJdbcUrl() + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&useLocalSessionState=true&useLocalTransactionState=true&cachePrepStmts=false&useServerPrepStmts=false";
    }

    @Test
    public void testMySql6Connection() throws SQLException {
        try (Connection connection = DriverManager.getConnection(getClientSideConnectionString(),
                                                                 mysqlServerContainer.getUsername(),
                                                                 mysqlServerContainer.getPassword())) {

            Assertions.assertInstanceOf(ConnectionImpl.class, connection);

            // The connection class has been instrumented
            Assertions.assertInstanceOf(IBithonObject.class, connection);
            Object connectionContext = ((IBithonObject) connection).getInjectedObject();

            ConnectionContext ctx = new ConnectionContext(mysqlServerContainer.getJdbcUrl(),
                                                          mysqlServerContainer.getUsername(),
                                                          "mysql");

            // Since the 'connectionContext' is loaded in InterceptorClassLoader, but the ctx is loaded in system classloader,
            // so we cannot use 'equals' to compare them.
            Assertions.assertEquals(ctx.toString(), connectionContext.toString());
        }
    }

    @Test
    public void testMultipleOperationsAggregation() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
            getClientSideConnectionString(),
            mysqlServerContainer.getUsername(),
            mysqlServerContainer.getPassword()
        )) {
            // Clear any existing metrics
            SQLMetricStorage.getInstance().collect(MESSAGE_CONVERTER, 0, 0);

            // Insert 2 times
            try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, email) VALUES (?, ?)")) {
                stmt.setString(1, "agguser1");
                stmt.setString(2, "agguser1@example.com");
                stmt.executeUpdate();

                stmt.setString(1, "agguser2");
                stmt.setString(2, "agguser2@example.com");
                stmt.executeUpdate();
            }

            // Select 3 times
            try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM users WHERE username = ?")) {
                stmt.setString(1, "testuser1");
                try (ResultSet rs = stmt.executeQuery()) {
                    // Process result
                }

                stmt.setString(1, "testuser2");
                try (ResultSet rs = stmt.executeQuery()) {
                    // Process result
                }

                stmt.setString(1, "agguser1");
                try (ResultSet rs = stmt.executeQuery()) {
                    // Process result
                }
            }

            // Update 4 times
            try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE users SET email = ? WHERE username = ?")) {
                stmt.setString(1, "updated1@example.com");
                stmt.setString(2, "testuser1");
                stmt.executeUpdate();

                stmt.setString(1, "updated2@example.com");
                stmt.setString(2, "testuser2");
                stmt.executeUpdate();

                stmt.setString(1, "updated3@example.com");
                stmt.setString(2, "agguser1");
                stmt.executeUpdate();

                stmt.setString(1, "updated4@example.com");
                stmt.setString(2, "agguser2");
                stmt.executeUpdate();
            }

            // Delete 2 times
            try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM users WHERE username = ?")) {
                stmt.setString(1, "agguser1");
                stmt.executeUpdate();

                stmt.setString(1, "agguser2");
                stmt.executeUpdate();
            }

            // Collect metrics after all operations
            //noinspection unchecked
            Collection<IMeasurement> measurements = (Collection<IMeasurement>) SQLMetricStorage.getInstance()
                                                                                               .collect(MESSAGE_CONVERTER, 0, 0);

            logger.info("Reported metrics for multiple operations: {}", measurements);

            // Should have 4 aggregated measurements: INSERT, SELECT, UPDATE, DELETE
            Assertions.assertEquals(4, measurements.size());

            // Convert to map(key is the statement type) for easier assertions
            Map<String, IMeasurement> measurementMap = measurements.stream()
                                                                   .collect(java.util.stream.Collectors.toMap(
                                                                       m -> m.getDimensions().getValue(1), // Operation type is at index 1
                                                                       m -> m
                                                                   ));

            // Check INSERT measurement
            Assertions.assertTrue(measurementMap.containsKey("INSERT"));
            AbstractMetricStorage.Measurement insertMeasurement = (AbstractMetricStorage.Measurement) measurementMap.get("INSERT");
            Assertions.assertEquals(Dimensions.of(mysqlServerContainer.getJdbcUrl(),
                                                  "INSERT",
                                                  "", // traceId
                                                  "" // statement
                                    ),
                                    insertMeasurement.getDimensions());
            SQLMetrics insertMetrics = (SQLMetrics) insertMeasurement.getMetricAccessor();
            Assertions.assertEquals(2, insertMetrics.callCount); // 2 INSERT operations
            Assertions.assertEquals(0, insertMetrics.errorCount);

            // Check SELECT measurement
            Assertions.assertTrue(measurementMap.containsKey("SELECT"));
            AbstractMetricStorage.Measurement selectMeasurement = (AbstractMetricStorage.Measurement) measurementMap.get("SELECT");
            Assertions.assertEquals(Dimensions.of(mysqlServerContainer.getJdbcUrl(),
                                                  "SELECT",
                                                  "", // traceId
                                                  "" // statement
                                    ),
                                    selectMeasurement.getDimensions());
            SQLMetrics selectMetrics = (SQLMetrics) selectMeasurement.getMetricAccessor();
            Assertions.assertEquals(3, selectMetrics.callCount); // 3 SELECT operations
            Assertions.assertEquals(0, selectMetrics.errorCount);

            // Check UPDATE measurement
            Assertions.assertTrue(measurementMap.containsKey("UPDATE"));
            AbstractMetricStorage.Measurement updateMeasurement = (AbstractMetricStorage.Measurement) measurementMap.get("UPDATE");
            Assertions.assertEquals(Dimensions.of(mysqlServerContainer.getJdbcUrl(),
                                                  "UPDATE",
                                                  "", // traceId
                                                  "" // statement
                                    ),
                                    updateMeasurement.getDimensions());
            SQLMetrics updateMetrics = (SQLMetrics) updateMeasurement.getMetricAccessor();
            Assertions.assertEquals(4, updateMetrics.callCount); // 4 UPDATE operations
            Assertions.assertEquals(0, updateMetrics.errorCount);

            // Check DELETE measurement
            Assertions.assertTrue(measurementMap.containsKey("DELETE"));
            AbstractMetricStorage.Measurement deleteMeasurement = (AbstractMetricStorage.Measurement) measurementMap.get("DELETE");
            Assertions.assertEquals(Dimensions.of(mysqlServerContainer.getJdbcUrl(),
                                                  "DELETE",
                                                  "", // traceId
                                                  "" // statement
                                    ),
                                    deleteMeasurement.getDimensions());
            SQLMetrics deleteMetrics = (SQLMetrics) deleteMeasurement.getMetricAccessor();
            Assertions.assertEquals(2, deleteMetrics.callCount); // 2 DELETE operations
            Assertions.assertEquals(0, deleteMetrics.errorCount);
        }
    }
}
