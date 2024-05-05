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

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.client.protocol.decoder.MultiDecoder;

import java.util.concurrent.CompletableFuture;

/**
 * Hook the constructor
 * {@link org.redisson.client.protocol.CommandData#CommandData(CompletableFuture, MultiDecoder, Codec, RedisCommand, Object[])}
 * so that we wrap the promise to get notified when the command is completed
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/5 10:47
 */
public class CommandData$Ctor extends BeforeInterceptor {
    @Override
    public void before(AopContext aopContext) {
        CompletableFuture<?> promise = aopContext.getArgAs(0);
        if (promise instanceof CommandCompletionPromise) {
            return;
        }
        RedisCommand<?> redisCommand = aopContext.getArgAs(3);
        if (redisCommand == null) {
            return;
        }

        aopContext.getArgs()[0] = new CommandCompletionPromise<>(promise, redisCommand);
    }
}
