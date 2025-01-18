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

package org.bithon.agent.plugin.jdbc.clickhouse;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class ClickHousePlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // ClickHouseConnection
            forClass("com.clickhouse.jdbc.internal.ClickHouseConnectionImpl")
                .onConstructor()
                .andArgs("com.clickhouse.jdbc.internal.ClickHouseJdbcUrlParser$ConnectionInfo")
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.ClickHouseConnectionImpl$Ctor")
                .build(),

            // PreparedStatement
            forClass(" com.clickhouse.jdbc.internal.SqlBasedPreparedStatement")
                .onConstructor()
                .andArgs(2, "com.clickhouse.jdbc.parser.ClickHouseSqlStatement")
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.SqlBasedPreparedStatement$Ctor")

                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.JdbcPreparedStatement$Execute")

                .build(),

            forClass(" com.clickhouse.jdbc.internal.InputBasedPreparedStatement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.InputBasedPreparedStatement$Execute")
                .build(),

            forClass(" com.clickhouse.jdbc.internal.TableBasedPreparedStatement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.TableBasedPreparedStatement$Execute")
                .build(),

            // Statement
            forClass("com.clickhouse.jdbc.internal.ClickHouseStatementImpl")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate", "executeLargeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andArgs(0, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.ClickHouseStatementImpl$Execute")

                .onMethod(Matchers.names("executeBatch", "executeLargeBatch"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.ClickHouseStatementImpl$ExecuteBatch")

                .build()
        );
    }
}
