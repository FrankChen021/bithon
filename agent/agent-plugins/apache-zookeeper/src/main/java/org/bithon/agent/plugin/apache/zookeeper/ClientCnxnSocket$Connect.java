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

package org.bithon.agent.plugin.apache.zookeeper;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

import java.net.InetSocketAddress;

/**
 * The {@link org.apache.zookeeper.ClientCnxnSocket} and its method, which returns the remote address,
 * are all defined as package level visibility.
 * To get the remote address simpler (to avoid Reflection which might fail in JDK 9 and above in which module is introduced),
 * we inject the address to the socket object itself.
 * <p>
 * <3.5 {@link org.apache.zookeeper.ClientCnxnSocketNIO#connect(InetSocketAddress)}
 * 3.5+ {@link org.apache.zookeeper.ClientCnxnSocketNetty#connect(InetSocketAddress)}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 6:10 pm
 */
public class ClientCnxnSocket$Connect extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        InetSocketAddress serverAddress = aopContext.getArgAs(0);
        if (serverAddress != null) {
            IBithonObject clientSocket = aopContext.getTargetAs();
            clientSocket.setInjectedObject(serverAddress.getHostString() + ":" + serverAddress.getPort());
        }
    }
}
