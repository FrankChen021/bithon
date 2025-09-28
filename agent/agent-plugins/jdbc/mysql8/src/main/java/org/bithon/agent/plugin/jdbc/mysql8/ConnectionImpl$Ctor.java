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

package org.bithon.agent.plugin.jdbc.mysql8;


import com.mysql.cj.conf.HostInfo;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.jdbc.common.ConnectionContext;

/**
 * {@link com.mysql.cj.jdbc.ConnectionImpl#ConnectionImpl(HostInfo)}
 * <p>
 * Get the cleanup connection string once the connection is set up to improve performance
 *
 * @author frank.chen021@outlook.com
 * @date 28/9/25 11:53 am
 */
public class ConnectionImpl$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        HostInfo hostInfo = aopContext.getArgAs(0);

        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(
            new ConnectionContext(
                "jdbc:mysql://" + hostInfo.getHost() + ":" + hostInfo.getPort() + "/" + hostInfo.getDatabase(),
                hostInfo.getUser(),
                "mysql"
            ));
    }
}
