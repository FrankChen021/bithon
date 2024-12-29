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

package org.bithon.agent.plugin.jdbc.postgresql;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class PostgresqlPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // PreparedStatement
            forClass("org.postgresql.jdbc.PgPreparedStatement")
                .onConstructor()
                .andArgs(1, "org.postgresql.core.CachedQuery")
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgPreparedStatement$Ctor")
                
                .onMethod("execute")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgPreparedStatement$Execute")

                .onMethod("executeQuery")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgPreparedStatement$Execute")

                .onMethod("executeUpdate")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgPreparedStatement$Execute")
                .build(),

            //
            // Statement
            //
            forClass("org.postgresql.jdbc.PgStatement")
                .onMethod("executeInternal")
                .andArgs("java.lang.String", "boolean")
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgStatement$Execute")

                .onMethod("executeQuery")
                .andArgs("java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgStatement$Execute")

                .onMethod("executeUpdateInternal")
                .andArgs("java.lang.String", "boolean", "boolean")
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgStatement$Execute")

                /* TODO:
                .onMethod("executeBatch")
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgStatement$ExecuteBatch")

                .onMethod("executeLargeBatch")
                .interceptedBy("org.bithon.agent.plugin.jdbc.postgresql.PgStatement$ExecuteBatch")
*/
                .build()
        );
    }
}
