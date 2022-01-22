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

package org.bithon.agent.plugin.jedis;

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class JedisPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Connection")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("connect")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.Connection$Connect")
                ),

            //2.9.x
            forClass("redis.clients.jedis.Client")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("readProtocolWithCheckingBroken")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.Client$ReadProtocol"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("sendCommand",
                                                                    "redis.clients.jedis.Protocol$Command", "[[B")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.Client$SendCommand"),

                    //3.x
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("sendCommand",
                                                                    "redis.clients.jedis.commands.ProtocolCommand",
                                                                    "[[B")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.Client$SendCommand")
                ),

            forClass("redis.clients.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("flushBuffer")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisOutputStream$FlushBuffer")
                ),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("flushBuffer")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisOutputStream$FlushBuffer")
                ),

            //2.9.x
            forClass("redis.clients.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("ensureFill")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisInputStream$EnsureFill")
                ),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("ensureFill")
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisInputStream$EnsureFill")
                )
        );
    }
}
