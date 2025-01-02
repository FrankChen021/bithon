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

package org.bithon.agent.plugin.jdbc.alibaba.druid;

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
public class DruidPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.alibaba.druid.pool.DruidDataSource")
                .onMethod("init")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidDataSource$Init")

                .onMethod("close")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidDataSource$Close")

                .onMethod("restart")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidDataSource$Restart")

                .onMethod("getStatValueAndReset")
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidDataSource$GetValueAndReset")

                .onMethod("getConnectionInternal")
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidDataSource$GetConnectionInternal")
                .build(),

            forClass("com.alibaba.druid.pool.DruidPooledConnection")
                .onMethod("close")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidPooledConnection$Close")
                .build(),

            forClass("com.alibaba.druid.pool.DruidPooledPreparedStatement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")
                .build(),

            forClass("com.alibaba.druid.pool.DruidPooledStatement")
                .onMethod(Matchers.names("execute", "executeQuery", "executeUpdate", "executeLargeUpdate"))
                .andVisibility(Visibility.PUBLIC)
                .andArgs(0, "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethod(Matchers.names("executeBatch", "executeLargeBatch"))
                .andVisibility(Visibility.PUBLIC)
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.jdbc.alibaba.druid.interceptor.DruidPooledStatement$ExecuteBatch")
                .build()
        );
    }
}
