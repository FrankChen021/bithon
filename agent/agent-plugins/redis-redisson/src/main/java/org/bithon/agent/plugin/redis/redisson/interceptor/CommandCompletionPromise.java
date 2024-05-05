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
import org.redisson.client.protocol.QueueCommand;
import org.redisson.client.protocol.RedisCommand;

import java.util.concurrent.CompletableFuture;

/**
 * For a normal command execution, the promise is executed in the {@link org.redisson.client.handler.CommandDecoder#decode(ChannelHandlerContext, ByteBuf, QueueCommand, int)},
 * which is hooked by {@link CommandDecoder$Decode}.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/5 10:49
 */
public class CommandCompletionPromise<T> extends CompletableFuture<T> {
    public CommandCompletionPromise(CompletableFuture<T> delegate, RedisCommand<?> command) {
        this.whenComplete((result, error) -> {
            try {
                CommandContext commandContext = (CommandContext) ((IBithonObject) command).getInjectedObject();
                if (commandContext != null) {
                    commandContext.complete(error != null);
                }
            } catch (Throwable ignored) {
                // Catch all exceptions to avoid breaking the original promise
            }

            // Relay the result to the original promise
            if (error != null) {
                delegate.completeExceptionally(error);
            } else {
                delegate.complete(result);
            }
        });
    }
}
