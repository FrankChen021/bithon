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

package org.bithon.agent.plugin.mysql;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;
import static org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition.isClassDefined;
import static org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition.or;

/**
 * @author frankchen
 */
public class MySqlPlugin implements IPlugin {
    public static final String METHOD_EXECUTE = "execute";
    public static final String METHOD_EXECUTE_QUERY = "executeQuery";
    public static final String METHOD_EXECUTE_UPDATE = "executeUpdate";
    public static final String METHOD_EXECUTE_INTERNAL = "executeInternal";
    public static final String METHOD_EXECUTE_UPDATE_INTERNAL = "executeUpdateInternal";
    public static final String METHOD_SEND_COMMAND = "sendCommand";
    private static final String OLD_VERSION_CONNECTION_CLASS = "com.mysql.jdbc.ConnectionImpl";
    private static final String NEW_VERSION_CONNECTION_CLASS = "com.mysql.cj.jdbc.ConnectionImpl";
    private static final String OLD_VERSION_STATEMENT_CLASS = "com.mysql.jdbc.StatementImpl";
    private static final String NEW_VERSION_STATEMENT_CLASS = "com.mysql.cj.jdbc.StatementImpl";

    private static final String[] STATEMENT_EXECUTE_ARGUMENTS = new String[]{"java.lang.String", "boolean"};
    private static final String[] STATEMENT_EXECUTE_QUERY_ARGUMENTS = new String[]{"java.lang.String"};
    private static final String[] STATEMENT_EXECUTE_UPDATE_ARGUMENTS = new String[]{
        "java.lang.String", "boolean",
        "boolean"
    };

    private static final String[] MYSQL_IO_SEND_COMMAND_ARGUMENTS = new String[]{
        "int", "java.lang.String",
        "com.mysql.jdbc.Buffer", "boolean",
        "java.lang.String", "int"
    };

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return or(
            // mysql 5
            isClassDefined("org.gjt.mm.mysql.Driver"),
            // mysql 6
            isClassDefined("com.mysql.cj.x.package-info")
        );
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            // metrics
            forClass("com.mysql.jdbc.MysqlIO")
                .onMethod(METHOD_SEND_COMMAND)
                .andArgs(MYSQL_IO_SEND_COMMAND_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.MySqlIOInterceptor")

                .onMethod("readAllResults")
                .andArgs("com.mysql.jdbc.StatementImpl",
                         "int", "int", "int", "boolean",
                         "java.lang.String",
                         "com.mysql.jdbc.Buffer",
                         "boolean", "long",
                         "[Lcom.mysql.jdbc.Field;")
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.MySqlIOInterceptor")
                .build(),


            forClass("com.mysql.jdbc.PreparedStatement")
                //
                // metrics
                //
                .onMethod(METHOD_EXECUTE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor")

                //
                // trace
                //
                .onMethod(METHOD_EXECUTE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")
                .build(),


            forClass("com.mysql.cj.jdbc.PreparedStatement")

                .onMethod(METHOD_EXECUTE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor")

                //
                // trace
                //
                .onMethod(METHOD_EXECUTE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")
                .build(),

            forClass(OLD_VERSION_STATEMENT_CLASS)
                //
                // metrics
                //
                .onMethod(METHOD_EXECUTE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andArgs(STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                //
                // trace
                //
                .onMethod(METHOD_EXECUTE)
                .andArgs("java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andArgs(STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")
                .build(),

            forClass(NEW_VERSION_STATEMENT_CLASS)
                //
                // metrics
                //
                .onMethod(METHOD_EXECUTE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andArgs(STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor")

                //
                // trace
                //
                .onMethod(METHOD_EXECUTE)
                .andArgs("java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_QUERY)
                .andArgs(STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")

                .onMethod(METHOD_EXECUTE_UPDATE_INTERNAL)
                .andArgs(STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")
                .build(),

            //
            // trace
            //
            forClass(OLD_VERSION_CONNECTION_CLASS)
                .onMethod("prepareStatement")
                .andArgs("java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "[I")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "[Ljava.lang.String;")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "int", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "int", "int", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")
                .build(),

            forClass(NEW_VERSION_CONNECTION_CLASS)
                .onMethod("prepareStatement")
                .andArgs("java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "[I")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "[Ljava.lang.String;")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "int", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")

                .onMethod("prepareStatement")
                .andArgs("java.lang.String", "int", "int", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")
                .build()
        );
    }
}
