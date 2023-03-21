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

import org.bithon.agent.core.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;
import static org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition.hasClass;
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
    private static final String OLD_VERSION_PREPARED_STATEMENT_CLASS = "com.mysql.jdbc.PreparedStatement";
    private static final String NEW_VERSION_PREPARED_STATEMENT_CLASS = "com.mysql.cj.jdbc.PreparedStatement";
    private static final String OLD_VERSION_CONNECTION_CLASS = "com.mysql.jdbc.ConnectionImpl";
    private static final String NEW_VERSION_CONNECTION_CLASS = "com.mysql.cj.jdbc.ConnectionImpl";
    private static final String OLD_VERSION_STATEMENT_CLASS = "com.mysql.jdbc.StatementImpl";
    private static final String NEW_VERSION_STATEMENT_CLASS = "com.mysql.cj.jdbc.StatementImpl";
    private static final String METHOD_READ_ALL_RESULTS = "readAllResults";

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
    private static final String[] MYSQL_IO_READ_ALL_RESULTS_ARGUMENTS = new String[]{
        "com.mysql.jdbc.StatementImpl",
        "int", "int", "int", "boolean",
        "java.lang.String",
        "com.mysql.jdbc.Buffer",
        "boolean", "long",
        "[Lcom.mysql.jdbc.Field;"
    };

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return or(
            // mysql 5
            hasClass("org.gjt.mm.mysql.Driver"),
            // mysql 6
            hasClass("com.mysql.cj.x.package-info")
        );
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            // metrics
            forClass("com.mysql.jdbc.MysqlIO")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_SEND_COMMAND,
                                                                    MYSQL_IO_SEND_COMMAND_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.MySqlIOInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_READ_ALL_RESULTS,
                                                                    MYSQL_IO_READ_ALL_RESULTS_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.MySqlIOInterceptor")
                ),


            forClass(OLD_VERSION_PREPARED_STATEMENT_CLASS)
                .methods(
                    //
                    // metrics
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor"),

                    //
                    // trace
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE)
                                                   .to("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                                                   .to("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                                                   .to("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")
                ),


            forClass(NEW_VERSION_PREPARED_STATEMENT_CLASS)
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.PreparedStatementInterceptor"),

                    //
                    // trace
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE)
                                                   .to("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                                                   .to("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                                                   .to("org.bithon.agent.plugin.mysql.trace.PreparedStatementTraceInterceptor")
                ),

            forClass(OLD_VERSION_STATEMENT_CLASS)
                .methods(
                    //
                    // metrics
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_INTERNAL,
                                                                    STATEMENT_EXECUTE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_QUERY,
                                                                    STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    //
                    // trace
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE, "java.lang.String")
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_QUERY,
                                                                    STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")
                ),

            forClass(NEW_VERSION_STATEMENT_CLASS)
                .methods(
                    //
                    // metrics
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_INTERNAL,
                                                                    STATEMENT_EXECUTE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_QUERY,
                                                                    STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.metrics.StatementInterceptor"),

                    //
                    // trace
                    //
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE, "java.lang.String")
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_INTERNAL,
                                                                    STATEMENT_EXECUTE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_QUERY,
                                                                    STATEMENT_EXECUTE_QUERY_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs(METHOD_EXECUTE_UPDATE_INTERNAL,
                                                                    STATEMENT_EXECUTE_UPDATE_ARGUMENTS)
                                                   .to("org.bithon.agent.plugin.mysql.trace.StatementTraceInterceptor")
                ),

            //
            // trace
            //
            forClass(OLD_VERSION_CONNECTION_CLASS)
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement", "java.lang.String")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement", "java.lang.String", "int")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement", "java.lang.String", "[I")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String",
                                                                    "[Ljava.lang.String;")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String",
                                                                    "int",
                                                                    "int")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String", "int", "int", "int")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")
                ),

            forClass(NEW_VERSION_CONNECTION_CLASS)
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String", "int")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String", "[I")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String", "[Ljava.lang.String;")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String", "int", "int")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("prepareStatement",
                                                                    "java.lang.String", "int", "int", "int")
                                                   .to("org.bithon.agent.plugin.mysql.trace.ConnectionTraceInterceptor")
                )
        );
    }
}
