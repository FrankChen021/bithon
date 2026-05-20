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

package org.bithon.agent.plugins.test.jdbc.sqlite;

import org.bithon.agent.instrumentation.aop.interceptor.installer.InstallerRecorder;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.jdbc.sqlite.SQLitePlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test case for SQLite JDBC plugin.
 *
 * @author frankchen
 */
public class SQLiteJdbcPluginTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new SQLitePlugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("org.xerial",
                             "sqlite-jdbc",
                             "3.50.3.0")
        );
    }

    @Test
    @Override
    public void testInterceptorInstallation() {
        super.testInterceptorInstallation();

        List<InstallerRecorder.InstrumentedMethod> instrumentedMethods = InstallerRecorder.INSTANCE.getInstrumentedMethods();
        assertInstrumented(instrumentedMethods,
                           "org.sqlite.jdbc3.JDBC3PreparedStatement",
                           "executeLargeUpdate",
                           "org.bithon.agent.plugin.jdbc.sqlite.JDBC3PreparedStatement$Execute");
        assertInstrumented(instrumentedMethods,
                           "org.sqlite.core.CorePreparedStatement",
                           "executeBatch",
                           "org.bithon.agent.plugin.jdbc.sqlite.CorePreparedStatement$ExecuteBatch");
        assertInstrumented(instrumentedMethods,
                           "org.sqlite.core.CorePreparedStatement",
                           "executeLargeBatch",
                           "org.bithon.agent.plugin.jdbc.sqlite.CorePreparedStatement$ExecuteBatch");
    }

    private static void assertInstrumented(List<InstallerRecorder.InstrumentedMethod> instrumentedMethods,
                                           String type,
                                           String method,
                                           String interceptor) {
        List<InstallerRecorder.InstrumentedMethod> matchingType = instrumentedMethods.stream()
                                                                                     .filter((m) -> type.equals(m.getType()))
                                                                                     .collect(Collectors.toList());
        Assertions.assertTrue(matchingType.stream()
                                          .anyMatch((m) -> method.equals(m.getMethodName())
                                                           && interceptor.equals(m.getInterceptorName())),
                              type + "#" + method + " should be instrumented by " + interceptor);
    }
}
