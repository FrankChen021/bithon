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

package org.bithon.agent.plugin.redis.lettuce;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.BithonClassDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2021-02-27 16:58:01
 */
public class LettucePlugin implements IPlugin {

    /**
     * Since no Connection objects are intercepted,
     * we have to instrument Connection objects to forward Endpoint info from RedisClient to Command objects
     * This makes {@link RedisClient$Connect} work
     */
    @Override
    public BithonClassDescriptor getBithonClassDescriptor() {
        return BithonClassDescriptor.of(
            "io.lettuce.core.StatefulRedisConnectionImpl",
            "io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl",
            "io.lettuce.core.pubsub.StatefulRedisClusterPubSubConnectionImpl",
            "io.lettuce.core.masterslave.StatefulRedisMasterSlaveConnectionImpl",
            "io.lettuce.core.sentinel.StatefulRedisSentinelConnectionImpl"
        );
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            //
            // add interceptor for forward Endpoint to Connection
            //
            forClass("io.lettuce.core.RedisClient")
                .onMethod("connect")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")

                .onMethod("connectAsync")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")

                .onMethod("connectPubSub")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")

                .onMethod("connectPubSubAsync")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")

                .onMethod("connectSentinel")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")

                .onMethod("connectSentinelAsync")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")
                .build(),

            forClass("io.lettuce.core.DefaultConnectionFuture")
                .onMethod("get")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.DefaultConnectionFuture$Get")

                .onMethod("join")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.DefaultConnectionFuture$Get")
                .build(),

            //
            // issue command
            //
            forClass("io.lettuce.core.AbstractRedisAsyncCommands")
                .onMethod("dispatch")
                .andArgs("io.lettuce.core.protocol.RedisCommand<K, V, T>")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.AbstractRedisAsyncCommands$Dispatch")
                .build(),

            //
            // request sync complete
            //
            forClass("io.lettuce.core.protocol.AsyncCommand")
                .onMethod("encode")
                .andArgs("io.netty.buffer.ByteBuf")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Encode")

                .onMethod("completeResult")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Complete")

                .onMethod("cancel")
                .andArgs("java.lang.boolean")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Complete")

                .onMethod("doCompleteExceptionally")
                .andArgs("java.lang.Throwable")
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Complete")
                .build(),

            forClass("io.lettuce.core.protocol.CommandHandler")
                .onMethod(Matchers.name("decode")
                                  .and(Matchers.takesArgument(0, "io.netty.buffer.ByteBuf"))
                                  .and(Matchers.takesArgument(1, "io.lettuce.core.protocol.RedisCommand")))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.CommandHandler$Decode")
                .build(),

            // Trace Support for Lettuce
            // Only works on Spring Data Redis
            // because this layer is a synchronized interface while the lettuce provides async operations
            forClass("org.springframework.data.redis.connection.lettuce.LettuceConnection")
                .onConstructor(Matchers.takesArguments(4).and(Matchers.takesArgument(0, "io.lettuce.core.api.StatefulConnection")))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.LettuceConnection$Ctor")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceGeoCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisGeoCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceHashCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisHashCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceHyperLogLogCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisHyperLogLogCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceKeyCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisKeyCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceListCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisListCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceScriptingCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisScriptingCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceSetCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisSetCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceServerCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisServerCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceStreamCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisStreamCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceStringCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisStringCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build(),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceZSetCommands")
                .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisZSetCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                .build()
        );
    }
}
