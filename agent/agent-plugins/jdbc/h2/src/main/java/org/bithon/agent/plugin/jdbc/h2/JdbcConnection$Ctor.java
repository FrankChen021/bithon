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
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;
import org.h2.engine.Session;
import org.h2.jdbc.JdbcConnection;

import java.util.Properties;

/**
 * {@link org.h2.jdbc.JdbcConnection#JdbcConnection(JdbcConnection)} constructor interceptor
 * {@link org.h2.jdbc.JdbcConnection#JdbcConnection(Session, String, String)}
 * {@link org.h2.jdbc.JdbcConnection#JdbcConnection(String, Properties, String, Object, boolean)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/30 16:17
 */
public class JdbcConnection$Ctor extends AfterInterceptor {

    /**
     * Inject ConnectionContext on the connection object which will be used by statement interceptors
     */
    @Override
    public void after(AopContext aopContext) throws Exception {
        if (aopContext.hasException()) {
            return;
        }

        JdbcConnection connection = aopContext.getTargetAs();
        ((IBithonObject) connection).setInjectedObject(new ConnectionContext(connection.getMetaData().getURL(),
                                                                             connection.getMetaData().getUserName(),
                                                                             "h2"));
    }
}
