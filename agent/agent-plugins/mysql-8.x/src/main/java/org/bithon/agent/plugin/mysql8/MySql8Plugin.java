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

package org.bithon.agent.plugin.mysql8;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class MySql8Plugin implements IPlugin {
    static final String METHOD_EXECUTE = "execute";
    static final String METHOD_EXECUTE_UPDATE = "executeUpdate";
    static final String METHOD_EXECUTE_INTERNAL = "executeInternal";
    static final String METHOD_EXECUTE_UPDATE_INTERNAL = "executeUpdateInternal";
    static final String METHOD_SEND_COMMAND = "sendCommand";

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return IInterceptorPrecondition.isClassDefined("com.mysql.cj.interceptors.QueryInterceptor");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // mysql-connector 8
            forClass("com.mysql.cj.jdbc.ClientPreparedStatement")
                .onMethod("execute")
                .interceptedBy("org.bithon.agent.plugin.mysql8.PreparedStatementInterceptor")

                .onMethod("executeQuery")
                .interceptedBy("org.bithon.agent.plugin.mysql8.PreparedStatementInterceptor")

                .onMethod("executeUpdate")
                .interceptedBy("org.bithon.agent.plugin.mysql8.PreparedStatementInterceptor")
                .build(),

            //
            // IO
            //
            forClass("com.mysql.cj.protocol.a.NativeProtocol")
                .onMethod("sendCommand")
                .andArgs("com.mysql.cj.protocol.Message", "boolean", "int")
                .interceptedBy("org.bithon.agent.plugin.mysql8.NativeProtocolInterceptor")

                .onMethod("readAllResults")
                .andArgs("int", "boolean", "com.mysql.cj.protocol.a.NativePacketPayload", "boolean",
                         "com.mysql.cj.protocol.ColumnDefinition",
                         "com.mysql.cj.protocol.ProtocolEntityFactory")
                .interceptedBy("org.bithon.agent.plugin.mysql8.NativeProtocolInterceptor")
                .build(),

            //
            // statement
            //
            forClass("com.mysql.cj.jdbc.StatementImpl")
                .onMethod("executeInternal")
                .andArgs("java.lang.String", "boolean")
                .interceptedBy("org.bithon.agent.plugin.mysql8.StatementInterceptor")

                .onMethod("executeQuery")
                .andArgs("java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.mysql8.StatementInterceptor")

                .onMethod("executeUpdate")
                .andArgs("java.lang.String", "boolean", "boolean")
                .interceptedBy("org.bithon.agent.plugin.mysql8.StatementInterceptor")

                .onMethod("executeUpdateInternal")
                .andArgs("java.lang.String", "boolean", "boolean")
                .interceptedBy("org.bithon.agent.plugin.mysql8.StatementInterceptor")
                .build()
        );
    }
}
