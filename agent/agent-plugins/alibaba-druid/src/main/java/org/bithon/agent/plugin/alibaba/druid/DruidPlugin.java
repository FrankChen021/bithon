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

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("com.alibaba.druid.pool.DruidDataSource")
                .onMethod("init")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Init")

                .onMethod("close")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Close")

                .onMethod("restart")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$Restart")

                .onMethod("getStatValueAndReset")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidDataSource$GetValueAndReset")
                .build(),

            forClass("com.alibaba.druid.pool.DruidPooledPreparedStatement")
                .onMethod("execute")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .onMethod("executeQuery")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .onMethod("executeUpdate")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")

                .onMethod("executeBatch")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledPreparedStatement$Execute")
                .build(),


            forClass("com.alibaba.druid.pool.DruidPooledStatement")
                .onMethod("execute")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethod("executeQuery")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethod("executeUpdate")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")

                .onMethod("executeBatch")
                .interceptedBy("org.bithon.agent.plugin.alibaba.druid.interceptor.DruidPooledStatement$Execute")
                .build()
        );
    }
}
