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

import org.apache.zookeeper.ClientWatchManager;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.client.HostProvider;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

/**
 * {@link org.apache.zookeeper.ClientCnxn#ClientCnxn(String, HostProvider, int, ZooKeeper, ClientWatchManager, org.apache.zookeeper.ClientCnxnSocket, long, byte[], boolean)}
 * {@link org.apache.zookeeper.ClientCnxn#ClientCnxn(String, HostProvider, int, ZooKeeper, ClientWatchManager, org.apache.zookeeper.ClientCnxnSocket, boolean)}
 * <p>
 * 3.7+ {@link org.apache.zookeeper.ClientCnxn#ClientCnxn(String, HostProvider, int, org.apache.zookeeper.client.ZKClientConfig, Watcher, org.apache.zookeeper.ClientCnxnSocket, boolean)}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 3:58 pm
 */
public class ClientCnxn$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        IBithonObject clientCnxn = aopContext.getTargetAs();

        // Inject context object for further use
        clientCnxn.setInjectedObject(new ZKConnectionContext(aopContext.getArgAs(5)));
    }
}
