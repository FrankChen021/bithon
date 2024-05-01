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
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
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
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connect")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connectAsync")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connectPubSub")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connectPubSubAsync")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connectSentinel")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("connectSentinelAsync")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.RedisClient$Connect")
                        ),

            forClass("io.lettuce.core.DefaultConnectionFuture")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("get")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.DefaultConnectionFuture$Get"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("join")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.DefaultConnectionFuture$Get")
                        ),

            //
            // issue command
            //
            forClass("io.lettuce.core.AbstractRedisAsyncCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("dispatch",
                                                                    "io.lettuce.core.protocol.RedisCommand<K, V, T>")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.AbstractRedisAsyncCommands$Dispatch")
                        ),

            //
            // request sync complete
            //
            forClass("io.lettuce.core.protocol.AsyncCommand")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("encode", "io.netty.buffer.ByteBuf")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Encode"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("completeResult")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Complete"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("cancel", "java.lang.boolean")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Complete"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("doCompleteExceptionally", "java.lang.Throwable")
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.AsyncCommand$Complete")

                        ),

            forClass("io.lettuce.core.protocol.CommandHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("decode")
                                                                     .and(Matchers.takesArgument(0, "io.netty.buffer.ByteBuf"))
                                                                     .and(Matchers.takesArgument(1, "io.lettuce.core.protocol.RedisCommand")))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.CommandHandler$Decode")
                        ),

            // Trace Support for Lettuce
            // Only works on Spring Data Redis
            // because this layer is a synchronized interface while the lettuce provides async operations
            forClass("org.springframework.data.redis.connection.lettuce.LettuceConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(4).and(Matchers.takesArgument(0, "io.lettuce.core.api.StatefulConnection")))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.LettuceConnection$Ctor")
                        ),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceGeoCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisGeoCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceHashCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisHashCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceHyperLogLogCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisHyperLogLogCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceKeyCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisKeyCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceListCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisListCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceScriptingCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisScriptingCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceSetCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisSetCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceServerCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisServerCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceStreamCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisStreamCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),
            forClass("org.springframework.data.redis.connection.lettuce.LettuceStringCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisStringCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        ),

            forClass("org.springframework.data.redis.connection.lettuce.LettuceZSetCommands")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("org.springframework.data.redis.connection.RedisZSetCommands"))
                                                   .to("org.bithon.agent.plugin.redis.lettuce.interceptor.Command$Execute")
                        )
                            );
    }
}
