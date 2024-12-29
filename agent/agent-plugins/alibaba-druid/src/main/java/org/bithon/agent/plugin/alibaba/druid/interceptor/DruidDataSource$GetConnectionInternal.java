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

package org.bithon.agent.plugin.alibaba.druid.interceptor;

import com.alibaba.druid.pool.DruidDataSource;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.alibaba.druid.ConnectionContext;

/**
 * the user name is injected to this object so that it can be used in PreparedStatement or Statement interceptors
 * <p>
 * {@link com.alibaba.druid.pool.DruidDataSource#getConnectionInternal(long)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 21:38
 */
public class DruidDataSource$GetConnectionInternal extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        DruidDataSource dataSource = aopContext.getTargetAs();
        IBithonObject connection = aopContext.getReturningAs();
        if (connection != null) {
            connection.setInjectedObject(new ConnectionContext(dataSource.getRawJdbcUrl(),
                                                               dataSource.getUsername()));
        }
    }
}
