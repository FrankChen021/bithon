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

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.BithonClassDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
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
    public BithonClassDescriptor getBithonClassDescriptor() {
        return BithonClassDescriptor.of("org.redisson.client.protocol.RedisCommand");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("org.redisson.client.handler.CommandEncoder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("encode")
                                                                     .and(Matchers.takesArguments(3))
                                                                     .and(Matchers.takesArgument(2, "io.netty.buffer.ByteBuf")))
                                                   .to("org.bithon.agent.plugin.redis.redisson.interceptor.CommandEncoder$Encode")
                        ),

            forClass("org.redisson.client.handler.CommandDecoder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("decode")
                                                                     .and(Matchers.takesArguments(4))
                                                                     .and(Matchers.takesArgument(1, "io.netty.buffer.ByteBuf"))
                                                                     .and(Matchers.takesArgument(2, "org.redisson.client.protocol.QueueCommand")))
                                                   .to("org.bithon.agent.plugin.redis.redisson.interceptor.CommandDecoder$Decode")
                        ),

            forClass("org.redisson.connection.ConnectionsHolder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndRawArgs("acquireConnection", "org.redisson.client.protocol.RedisCommand")
                                                   .to("org.bithon.agent.plugin.redis.redisson.interceptor.ConnectionsHolder$AcquireConnection")
                        ),

            forClass("org.redisson.spring.data.connection.RedissonConnection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("read")
                                                   .to("org.bithon.agent.plugin.redis.redisson.interceptor.RedissonConnection$Read")
                        )


                            );
    }
}
