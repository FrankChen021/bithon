/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class JedisPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Connection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("connect")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisConnectionConnect")
                ),

            //2.9.x
            forClass("redis.clients.jedis.Client")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("readProtocolWithCheckingBroken")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisClientReadProtocol"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("sendCommand",
                                                                    "redis.clients.jedis.Protocol$Command", "[[B")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisClientSendCommand"),

                    //3.x
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("sendCommand",
                                                                    "redis.clients.jedis.commands.ProtocolCommand",
                                                                    "[[B")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisClientSendCommand")
                ),

            forClass("redis.clients.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("flushBuffer")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisOutputStreamFlushBuffer")
                ),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("flushBuffer")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisOutputStreamFlushBuffer")
                ),

            //2.9.x
            forClass("redis.clients.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("ensureFill")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisInputStreamEnsureFill")
                ),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("ensureFill")
                                                   .to("com.sbss.bithon.agent.plugin.jedis.interceptor.JedisInputStreamEnsureFill")
                )
        );
    }
}
