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
public class PrestoPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            // PrestoConnection
            forClass("io.prestosql.jdbc.PrestoConnection")
                .onConstructor()
                .interceptedBy("org.bithon.agent.plugin.jdbc.presto.PrestoConnection$Ctor")
                .build(),

            // PreparedStatement
            forClass("io.prestosql.jdbc.PrestoPreparedStatement")
                .onConstructor()
                .andArgs(1, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.presto.PrestoPreparedStatement$Ctor")

                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.presto.PrestoPreparedStatement$Execute")

                .build(),

            // Statement
            forClass("io.prestosql.jdbc.PrestoStatement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate", "executeLargeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andArgs(0, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.presto.PrestoStatement$Execute")

                .onMethod(Matchers.names("executeBatch", "executeLargeBatch"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.presto.PrestoStatement$ExecuteBatch")

                .build()
        );
    }
}

