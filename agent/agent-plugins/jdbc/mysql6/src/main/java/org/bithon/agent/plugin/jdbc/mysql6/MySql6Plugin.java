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

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;
import static org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition.isClassDefined;

/**
 * @author frankchen
 */
public class MySql6Plugin implements IPlugin {
    public static final String METHOD_SEND_COMMAND = "sendCommand";

    @Override
    public IInterceptorPrecondition getPreconditions() {
        // Only exists in mysql 6
        return isClassDefined("com.mysql.cj.x.package-info");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            /*
            forClass("com.mysql.cj.mysqla.io.MysqlaProtocol")
                .onMethod("sendCommand")
                .andArgs("int",
                         "java.lang.String",
                         "com.mysql.jdbc.Buffer",
                         "boolean",
                         "java.lang.String",
                         "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.MySqlIOInterceptor")

                .onMethod("readAllResults")
                .andArgs("com.mysql.jdbc.StatementImpl",
                         "int", "int", "int", "boolean",
                         "java.lang.String",
                         "com.mysql.jdbc.Buffer",
                         "boolean", "long",
                         "[Lcom.mysql.jdbc.Field;")
                .interceptedBy("org.bithon.agent.plugin.jdbc.6.metrics.MySqlIOInterceptor")
                .build(),
                */

            // PreparedStatement
            forClass("com.mysql.cj.jdbc.PreparedStatement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.mysql6.PreparedStatement$Execute")
                .build(),

            // Statement
            forClass("com.mysql.cj.jdbc.StatementImpl")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate", "executeLargeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andArgs(0, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.mysql6.StatementImpl$Execute")

                .onMethod(Matchers.names("executeBatch", "executeLargeBatch"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.mysql6.StatementImpl$ExecuteBatch")

                .build()
        );
    }
}
