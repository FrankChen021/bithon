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
                .hook()
                .onMethodAndNoArgs("init")
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Init")

                .hook()
                .onMethodAndNoArgs("close")
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Close")

                .hook()
                .onMethodAndNoArgs("restart")
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Restart")

                .hook()
                .onMethodName("getStatValueAndReset")
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$GetValueAndReset")
                .build(),

            forClass("com.alibaba.druid.pool.DruidPooledPreparedStatement")
                .hook()
                .onMethodAndNoArgs(METHOD_EXECUTE)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .hook()
                .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .hook()
                .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .hook()
                .onMethodAndNoArgs(METHOD_EXECUTE_BATCH)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")
                .build(),


            forClass("com.alibaba.druid.pool.DruidPooledStatement")
                .hook()
                .onMethodName(METHOD_EXECUTE)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .hook()
                .onMethodName(METHOD_EXECUTE_QUERY)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .hook()
                .onMethodName(METHOD_EXECUTE_UPDATE)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .hook()
                .onMethodName(METHOD_EXECUTE_BATCH)
                .to("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")
                .build()
        );
    }
}
