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
import org.bithon.component.commons.utils.ReflectionUtils;

import java.nio.ByteBuffer;

/**
 * {@link org.apache.zookeeper.ClientCnxn.Packet#createBB()}
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 10:24 pm
 */
public class ClientCnxnPacket$CreateBB extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        ByteBuffer byteBuffer = (ByteBuffer) ReflectionUtils.getFieldValue(aopContext.getTarget(), "bb");
        if (byteBuffer != null) {
            int bytesSent = byteBuffer.remaining();
            if (bytesSent > 0) {
                Object requestHeader = ReflectionUtils.getFieldValue(aopContext.getTarget(), "requestHeader");
                if (requestHeader instanceof IBithonObject) {
                    // The IOContext is injected in RequestHeader$Ctor
                    IOMetrics ioMetrics = (IOMetrics) ((IBithonObject) requestHeader).getInjectedObject();
                    ioMetrics.bytesSent = bytesSent;
                }
            }
        }
    }
}
