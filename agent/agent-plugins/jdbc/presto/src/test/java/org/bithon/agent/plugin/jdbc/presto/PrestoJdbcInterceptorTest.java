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

package org.bithon.agent.plugin.jdbc.presto;

import io.prestosql.jdbc.$internal.guava.collect.ImmutableMap;
import io.prestosql.jdbc.PrestoConnection;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InterceptorInstaller;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.exporter.InMemoryMessageExporterFactory;
import org.bithon.agent.observability.metric.domain.sql.SQLMetricStorage;
import org.bithon.agent.observability.utils.MiscUtils;
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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

public class PrestoJdbcInterceptorTest {

    private static final Logger logger = LoggerFactory.getLogger(PrestoJdbcInterceptorTest.class);

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
                                                       "-Dbithon.application.port=9897"
                             ));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(ImmutableMap.of(
                                 "bithon_exporters_tracing_client_factory", InMemoryMessageExporterFactory.class.getName(),
                                 "bithon_exporters_metric_client_factory", InMemoryMessageExporterFactory.class.getName(),
                                 "bithon_tracing_debug", "true"
                             ));

            ConfigurationManager.createForTesting(new File("not-exists"));
        }

        //
        // Install interceptors
        //
        InterceptorInstaller.install(new PrestoPlugin(), PrestoJdbcInterceptorTest.class.getClassLoader());
    }

    @AfterAll
    static void tearDown() {
    }

    private String getJdbcUrl() {
        return "jdbc:presto://presto-gw.com:443/hive/shopee?SSL=true";
    }

    private final String user = "admin";
    private final String pwd = "password";

    @BeforeEach
    void beforeEach() {
        // Collect previous metrics to clean up
        SQLMetricStorage.getInstance().collect(MESSAGE_CONVERTER, 0, 0);

        InMemoryMessageExporterFactory.InMemoryTracingExporter.clear();
    }

    @Test
    public void testMySql8Connection() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
            getJdbcUrl(),
            user,
            pwd
        )) {
            Assertions.assertInstanceOf(PrestoConnection.class, connection);

            // The connection class has been instrumented
            Assertions.assertInstanceOf(IBithonObject.class, connection);
            Object connectionContext = ((IBithonObject) connection).getInjectedObject();

            ConnectionContext ctx = new ConnectionContext(MiscUtils.cleanupConnectionString(getJdbcUrl()),
                                                          user,
                                                          "presto");
            // Since the 'connectionContext' is loaded in InterceptorClassLoader, but the ctx is loaded in system classloader,
            // so we cannot use 'equals' to compare them.
            Assertions.assertEquals(ctx.toString(), connectionContext.toString());
        }
    }
}
