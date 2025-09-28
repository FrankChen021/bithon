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
import org.bithon.agent.observability.exporter.InMemoryMessageExporterFactory;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
import java.sql.SQLException;
import java.util.Arrays;

public class MySql6InterceptorTest {

    private static final Logger logger = LoggerFactory.getLogger(MySql6InterceptorTest.class);

    private static MySQLContainer<?> mysqlServerContainer;

    @BeforeAll
    static void setUpDatabase() {
        //
        // Initialize agent to install interceptors first
        //
        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs).thenReturn(Arrays.asList("-Dbithon.application.name=test", "-Dbithon.application.env=local", "-Dbithon.application.port=9897"));

            configurationMock.when(Helper::getEnvironmentVariables).thenReturn(ImmutableMap.of("bithon_exporters_tracing_client_factory", InMemoryMessageExporterFactory.class.getName(), "bithon_tracing_debug", "true"));

            ConfigurationManager.createForTesting(new File("not-exists"));
        }

        //
        // Install interceptors
        //
        InterceptorInstaller.install(new MySql6Plugin(), MySql6InterceptorTest.class.getClassLoader());

        //
        // Use 5.7 server to be compatible with 6.x driver
        //
        //noinspection resource
        mysqlServerContainer = new MySQLContainer<>("mysql:5.7")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-mysql8.sql");

        logger.info("Starting MySQL container...");
        mysqlServerContainer.start();
    }

    @AfterAll
    static void tearDown() {
        logger.info("Stopping MySQL container");
        mysqlServerContainer.close();
    }

    @Test
    public void testMySql6Connection() throws SQLException {
        try (Connection connection = DriverManager.getConnection(mysqlServerContainer.getJdbcUrl(), mysqlServerContainer.getUsername(), mysqlServerContainer.getPassword())) {
            Assertions.assertInstanceOf(ConnectionImpl.class, connection);

            // The connection class has been instrumented
            Assertions.assertInstanceOf(IBithonObject.class, connection);
            Object connectionContext = ((IBithonObject) connection).getInjectedObject();

            ConnectionContext ctx = new ConnectionContext(mysqlServerContainer.getJdbcUrl(), mysqlServerContainer.getUsername(), "mysql");
            // Since the 'connectionContext' is loaded in InterceptorClassLoader, but the ctx is loaded in system classloader,
            // so we cannot use 'equals' to compare them.
            Assertions.assertEquals(ctx.toString(), connectionContext.toString());
        }
    }
}
