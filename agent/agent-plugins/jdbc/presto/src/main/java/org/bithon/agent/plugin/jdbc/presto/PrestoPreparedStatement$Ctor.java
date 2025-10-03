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

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.jdbc.common.StatementContext;

/**
 * {@link io.prestosql.jdbc.PrestoPreparedStatement} constructor interceptor
 *
 * @author frank.chen021@outlook.com
 * @date 2025/10/02
 */
public class PrestoPreparedStatement$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        String sql = aopContext.getArgAs(2);

        // Inject context so that they can be accessed in statement interceptors
        IBithonObject preparedStatement = aopContext.getTargetAs();
        preparedStatement.setInjectedObject(new StatementContext(sql));
    }
}

