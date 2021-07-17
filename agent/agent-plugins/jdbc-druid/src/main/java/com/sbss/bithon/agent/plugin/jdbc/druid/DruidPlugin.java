/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.jdbc.druid;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

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
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("init")
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidDataSourceInit"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("close")
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidDataSourceClose"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("restart")
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidDataSourceRestart"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("getStatValueAndReset")
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidDataSourceGetValueAndReset")
                ),

            forClass("com.alibaba.druid.pool.DruidPooledPreparedStatement")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_QUERY)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs(METHOD_EXECUTE_UPDATE)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor")
                ),

            forClass("com.alibaba.druid.pool.DruidPooledStatement")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods(METHOD_EXECUTE)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods(METHOD_EXECUTE_QUERY)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods(METHOD_EXECUTE_UPDATE)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods(METHOD_EXECUTE_BATCH)
                                                   .to("com.sbss.bithon.agent.plugin.jdbc.druid.interceptor.DruidSqlInterceptor")
                )

            /*,
                return TargetClassNameMatcher.byName(SQL_PREPAREDSTATEMENT_POINTCUT);
                return TargetMethodMatcher.byNameAndArgs(METHOD_EXECUTE, null);

                return TargetClassNameMatcher.byName(SQL_PREPAREDSTATEMENT_POINTCUT);
                return TargetMethodMatcher.byNameAndArgs(METHOD_EXECUTE_QUERY, null);

                return TargetClassNameMatcher.byName(SQL_PREPAREDSTATEMENT_POINTCUT);
                return TargetMethodMatcher.byNameAndArgs(METHOD_EXECUTE_UPDATE, null);

                return TargetClassNameMatcher.byName(SQL_STATEMENT_POINTCUT);
                return TargetMethodMatcher.byName(METHOD_EXECUTE);

                return TargetClassNameMatcher.byName(SQL_STATEMENT_POINTCUT);
                return TargetMethodMatcher.byName(METHOD_EXECUTE_QUERY);

                return TargetClassNameMatcher.byName(SQL_STATEMENT_POINTCUT);
                return TargetMethodMatcher.byName(METHOD_EXECUTE_UPDATE);

                return TargetClassNameMatcher.byName(SQL_STATEMENT_POINTCUT);
                return TargetMethodMatcher.byName(METHOD_EXECUTE_BATCH);
                }*/
        );
    }
}
