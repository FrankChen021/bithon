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
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.nio.ByteBuffer;

/**
 * {@link org.apache.zookeeper.ClientCnxn.SendThread#readResponse(ByteBuffer)}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 10:47 pm
 */
public class ClientCnxnSendThread$ReadResponse extends AroundInterceptor {
    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ByteBuffer incoming = aopContext.getArgAs(0);
        if (incoming == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        aopContext.setUserContext(incoming.position());
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) throws Exception {
        // The packet object is stored by ClientCnxn$FinishPacket interceptor
        Object packet = InterceptorContext.remove("apache.zookeeper.packet");
        if (packet == null) {
            return;
        }

        ByteBuffer incoming = aopContext.getArgAs(0);
        int readSize = incoming.position() - (int) aopContext.getUserContext();
        Object requestHeader = ReflectionUtils.getFieldValue(packet, "requestHeader");
        if (requestHeader instanceof IBithonObject) {
            // The IOContext is injected when the RequestHeader is initialized
            IOMetrics ioMetrics = (IOMetrics) ((IBithonObject) requestHeader).getInjectedObject();
            ioMetrics.bytesReceived = readSize;
        }
    }
}
