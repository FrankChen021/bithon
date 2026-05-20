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

package org.bithon.agent.plugin.jdbc.sqlite;

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
public class SQLitePlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // SQLiteConnection
            forClass("org.sqlite.SQLiteConnection")
                .onConstructor()
                .andArgsSize(3)
                .andArgs(0, "java.lang.String")
                .andArgs(1, "java.lang.String")
                .andArgs(2, "java.util.Properties")
                .interceptedBy("org.bithon.agent.plugin.jdbc.sqlite.SQLiteConnection$Ctor")
                .build(),

            // PreparedStatement
            forClass("org.sqlite.jdbc3.JDBC3PreparedStatement")
                .onConstructor()
                .andArgs(0, "org.sqlite.SQLiteConnection")
                .andArgs(1, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.sqlite.JDBC3PreparedStatement$Ctor")

                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.sqlite.JDBC3PreparedStatement$Execute")

                .build(),

            // Statement
            forClass("org.sqlite.jdbc3.JDBC3Statement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate", "executeLargeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andArgs(0, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.sqlite.JDBC3Statement$Execute")

                .onMethod(Matchers.names("executeBatch", "executeLargeBatch"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.sqlite.JDBC3Statement$ExecuteBatch")

                .build()
        );
    }
}
