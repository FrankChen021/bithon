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

package org.bithon.agent.plugin.jdbc.h2;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.h2.jdbc.JdbcConnection;

/**
 * {@link org.h2.jdbc.JdbcPreparedStatement#JdbcPreparedStatement(JdbcConnection, String, int, int, int, Object)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/30 16:14
 */
public class JdbcPreparedStatement$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        String sql = aopContext.getArgAs(1);

        // Inject context so that they can be accessed in statement interceptors
        IBithonObject preparedStatement = aopContext.getTargetAs();
        preparedStatement.setInjectedObject(sql);
    }
}
