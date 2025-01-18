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

package org.bithon.agent.plugin.jdbc.clickhouse.yandex;

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
public class YandexClickHousePlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // JdbcConnection
            forClass("ru.yandex.clickhouse.ClickHouseConnectionImpl")
                .onConstructor()
                .andArgsSize(2)
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.yandex.ClickHouseConnectionImpl$Ctor")
                .build(),

            // PreparedStatement
            forClass("ru.yandex.clickhouse.ClickHousePreparedStatementImpl")
                .onConstructor()
                .andArgs(3, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.yandex.ClickHousePreparedStatementImpl$Ctor")

                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.yandex.ClickHousePreparedStatementImpl$Execute")

                .build(),

            // Statement
            forClass("ru.yandex.clickhouse.ClickHouseStatementImpl")
                // execute is NOT instrumented because it calls executeQuery
                .onMethod(Matchers.names("executeQuery"))
                .andVisibility(Visibility.PUBLIC)
                // instrument the method with 4 arguments only 'cause the others call this one
                .andArgsSize(4)
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.yandex.ClickHouseStatementImpl$Execute")

                .onMethod(Matchers.names("executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andArgsSize(1)
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.yandex.ClickHouseStatementImpl$Execute")

                .onMethod(Matchers.names("executeBatch", "executeLargeBatch"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.clickhouse.yandex.ClickHouseStatementImpl$ExecuteBatch")

                .build()
        );
    }
}
