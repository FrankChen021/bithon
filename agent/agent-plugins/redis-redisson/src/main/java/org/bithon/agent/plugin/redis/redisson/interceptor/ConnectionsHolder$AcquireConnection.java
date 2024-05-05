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

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.RedisConnection;
import org.redisson.client.protocol.RedisCommand;

import java.util.concurrent.CompletableFuture;

/**
 * {@link org.redisson.connection.ConnectionsHolder#acquireConnection(RedisCommand)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/4 22:20
 */
public class ConnectionsHolder$AcquireConnection extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        CompletableFuture<? extends RedisConnection> future = aopContext.getReturningAs();
        if (future.isDone()) {
            try {
                attachConnectionContext(future.get(), aopContext.getArgAs(0));
            } catch (Exception ignored) {
            }
            return;
        }

        RedisCommand<?> redisCommand = aopContext.getArgAs(0);
        aopContext.setReturning(future.thenAccept((conn) -> attachConnectionContext(conn, redisCommand)));
    }

    private static void attachConnectionContext(RedisConnection connection, RedisCommand<?> redisCommand) {
        RedisClientConfig clientConfig = connection.getRedisClient().getConfig();
        String endpoint = clientConfig.getAddress().getHost() + ":" + clientConfig.getAddress().getPort();
        int dbIndex = clientConfig.getDatabase();
        ((IBithonObject) redisCommand).setInjectedObject(new CommandContext(endpoint, dbIndex));
    }
}
