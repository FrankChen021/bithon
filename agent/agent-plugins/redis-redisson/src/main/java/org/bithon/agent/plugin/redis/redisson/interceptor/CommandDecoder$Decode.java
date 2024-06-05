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

package org.bithon.agent.plugin.redis.redisson.interceptor;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.QueueCommand;

/**
 * Hook
 * {@link org.redisson.client.handler.CommandDecoder#decode(ChannelHandlerContext, ByteBuf, QueueCommand, int)}
 * to get the response size
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/4 22:01
 */
public class CommandDecoder$Decode extends BeforeInterceptor {

    @Override
    public void before(AopContext aopContext) {
        QueueCommand command = aopContext.getArgAs(2);
        if (!(command instanceof CommandData)) {
            return;
        }

        CommandContext commandContext = (CommandContext) ((IBithonObject) command).getInjectedObject();
        if (commandContext == null) {
            return;
        }

        ByteBuf buf = aopContext.getArgAs(1);
        aopContext.setUserContext(buf.readerIndex());
        commandContext.responseBuf = buf;
        commandContext.responseBufStartIndex = buf.readerIndex();
    }
}
