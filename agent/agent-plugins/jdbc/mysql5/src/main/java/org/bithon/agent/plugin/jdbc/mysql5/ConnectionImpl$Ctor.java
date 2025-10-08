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

package org.bithon.agent.plugin.jdbc.mysql5;


import com.mysql.jdbc.ConnectionImpl;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;

import java.util.Properties;

/**
 * {@link com.mysql.jdbc.ConnectionImpl#ConnectionImpl(String, int, Properties, String, String)}
 * <p>
 * Get the cleanup connection string once the connection is set up to improve performance
 *
 * @author frank.chen021@outlook.com
 * @date 28/9/25 20:46
 */
public class ConnectionImpl$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        String host = aopContext.getArgAs(0);
        int port = aopContext.getArgAs(1);
        String db = aopContext.getArgAs(3);

        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(
            new ConnectionContext(
                "jdbc:mysql://" + host + ":" + port + "/" + db,
                ((ConnectionImpl) aopContext.getTargetAs()).getUser(),
                "mysql"
            ));
    }
}
