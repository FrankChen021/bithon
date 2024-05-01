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

package org.bithon.agent.plugin.redis.jedis3;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class Jedis3Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        // Only exists in Jedis 3.x
        return IInterceptorPrecondition.hasClass("redis.clients.jedis.ClusterRest");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Jedis")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement(
                                                       "redis.clients.jedis.commands.JedisCommands",
                                                       "redis.clients.jedis.commands.MultiKeyCommands",
                                                       "redis.clients.jedis.commands.AdvancedJedisCommands",
                                                       "redis.clients.jedis.commands.ScriptingCommands",
                                                       "redis.clients.jedis.commands.BasicCommands",
                                                       "redis.clients.jedis.commands.ClusterCommands",
                                                       "redis.clients.jedis.commands.SentinelCommands",
                                                       "redis.clients.jedis.commands.ModuleCommands"))
                                                   .to("org.bithon.agent.plugin.redis.jedis3.interceptor.OnCommand")
                        ),

            forClass("redis.clients.jedis.BinaryJedis")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement("redis.clients.jedis.commands.BasicCommands",
                                                                                "redis.clients.jedis.commands.BinaryJedisCommands",
                                                                                "redis.clients.jedis.commands.MultiKeyBinaryCommands",
                                                                                "redis.clients.jedis.commands.AdvancedBinaryJedisCommands",
                                                                                "redis.clients.jedis.commands.BinaryScriptingCommands"))
                                                   .to("org.bithon.agent.plugin.redis.jedis3.interceptor.OnCommand")
                        ),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.OutputStream")))
                                                   .to("org.bithon.agent.plugin.redis.jedis3.interceptor.RedisOutputStream$Ctor")
                        ),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.InputStream")))
                                                   .to("org.bithon.agent.plugin.redis.jedis3.interceptor.RedisInputStream$Ctor")
                        ));
    }
}
