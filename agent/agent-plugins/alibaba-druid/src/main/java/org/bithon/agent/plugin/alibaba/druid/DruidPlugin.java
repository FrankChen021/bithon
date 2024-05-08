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

package org.bithon.agent.plugin.alibaba.druid;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class DruidPlugin implements IPlugin {
    public static final String METHOD_EXECUTE = "execute";
    public static final String METHOD_EXECUTE_QUERY = "executeQuery";
    public static final String METHOD_EXECUTE_UPDATE = "executeUpdate";
    public static final String METHOD_EXECUTE_BATCH = "executeBatch";

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.alibaba.druid.pool.DruidDataSource")
                .onMethodAndNoArgs("init")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Init")

                .onMethodAndNoArgs("close")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Close")

                .onMethodAndNoArgs("restart")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Restart")

                .onMethodName("getStatValueAndReset")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$GetValueAndReset")
                .build(),

            forClass("com.alibaba.druid.pool.DruidPooledPreparedStatement")
                .onMethodAndNoArgs(METHOD_EXECUTE)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .onMethodAndNoArgs(METHOD_EXECUTE_BATCH)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")
                .build(),


            forClass("com.alibaba.druid.pool.DruidPooledStatement")
                .onMethodName(METHOD_EXECUTE)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethodName(METHOD_EXECUTE_QUERY)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethodName(METHOD_EXECUTE_UPDATE)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethodName(METHOD_EXECUTE_BATCH)
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")
                .build()
        );
    }
}
