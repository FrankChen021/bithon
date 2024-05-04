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
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.plugin.redis.redisson.ConnectionContext;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.RedisCommand;

/**
 * {@link org.redisson.client.handler.CommandEncoder#encode(ChannelHandlerContext, CommandData, ByteBuf)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/4 20:55
 */
public class CommandEncoder$Encode extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ByteBuf buf = aopContext.getArgAs(2);
        aopContext.setUserContext(buf.writerIndex());
        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        int before = aopContext.getUserContext();
        ByteBuf after = aopContext.getArgAs(2);
        int size = after.writerIndex() - before;
        if (size <= 0) {
            return;
        }

        CommandData<?, ?> commandData = aopContext.getArgAs(1);
        RedisCommand<?> redisCommand = commandData.getCommand();
        ConnectionContext connectionContext = (ConnectionContext) ((IBithonObject) redisCommand).getInjectedObject();
        if (connectionContext != null) {
            connectionContext.requestBytes = size;
        }
    }
}
