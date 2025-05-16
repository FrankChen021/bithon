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

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;

/**
 * The method {@link org.apache.zookeeper.ClientCnxn#finishPacket(org.apache.zookeeper.ClientCnxn.Packet)}
 * is called inside {@link org.apache.zookeeper.ClientCnxn.SendThread#readResponse()},
 * So we save the Packet object on current thread so that it can be accessed by interceptor {@link ClientCnxnSendThread$ReadResponse}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 10:42 pm
 */
public class ClientCnxn$FinishPacket extends AfterInterceptor {

    /**
     * Keep the packet object on current thread so that it can be accessed by SendThread#readResponse
     */
    @Override
    public void after(AopContext aopContext) throws Exception {
        InterceptorContext.set("apache.zookeeper.packet", aopContext.getArgAs(0));
    }
}
