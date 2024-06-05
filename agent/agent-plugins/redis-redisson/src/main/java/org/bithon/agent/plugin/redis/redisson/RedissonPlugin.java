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

package org.bithon.agent.plugin.redis.redisson;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2024-05-04 20:45:01
 */
public class RedissonPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("org.redisson.client.handler.CommandEncoder")
                .onMethod("encode")
                .andArgsSize(3)
                .andArgs(2, "io.netty.buffer.ByteBuf")
                .interceptedBy("org.bithon.agent.plugin.redis.redisson.interceptor.CommandEncoder$Encode")
                .build(),

            forClass("org.redisson.client.handler.CommandDecoder")
                .onMethod("decode")
                .andArgsSize(4)
                .andArgs(1, "io.netty.buffer.ByteBuf")
                .andArgs(2, "org.redisson.client.protocol.QueueCommand")
                .interceptedBy("org.bithon.agent.plugin.redis.redisson.interceptor.CommandDecoder$Decode")
                .build(),

            forClass("org.redisson.client.protocol.CommandData")
                .onConstructor()
                .andArgs(0, "java.util.concurrent.CompletableFuture")
                .andArgs(3, "org.redisson.client.protocol.RedisCommand")
                .interceptedBy("org.bithon.agent.plugin.redis.redisson.interceptor.CommandData$Ctor")
                .build(),

            forClass("org.redisson.client.RedisConnection")
                .onMethod("send")
                .andRawArgs("org.redisson.client.protocol.CommandData")
                .interceptedBy("org.bithon.agent.plugin.redis.redisson.interceptor.RedisConnection$Send")
                .build());
    }
}
