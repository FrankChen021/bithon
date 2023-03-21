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

import org.bithon.agent.core.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class JedisPlugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        // < 4.0
        return IInterceptorPrecondition.hasClass("redis.clients.jedis.Client");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Jedis")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(ElementMatchers.isPublic()
                                                                            .and(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                                                                            .and(ElementMatchers.isOverriddenFrom(ElementMatchers.namedOneOf(
                                                                                // 2.9
                                                                                "redis.clients.jedis.JedisCommands",
                                                                                "redis.clients.jedis.MultiKeyCommands",
                                                                                "redis.clients.jedis.AdvancedJedisCommands",
                                                                                "redis.clients.jedis.ScriptingCommands",
                                                                                "redis.clients.jedis.BasicCommands",
                                                                                "redis.clients.jedis.ClusterCommands",
                                                                                "redis.clients.jedis.SentinelCommands",

                                                                                // 3.0
                                                                                "redis.clients.jedis.commands.JedisCommands",
                                                                                "redis.clients.jedis.commands.MultiKeyCommands",
                                                                                "redis.clients.jedis.commands.AdvancedJedisCommands",
                                                                                "redis.clients.jedis.commands.ScriptingCommands",
                                                                                "redis.clients.jedis.commands.BasicCommands",
                                                                                "redis.clients.jedis.commands.ClusterCommands",
                                                                                "redis.clients.jedis.commands.SentinelCommands",
                                                                                "redis.clients.jedis.commands.ModuleCommands",

                                                                                // 4.0
                                                                                "redis.clients.jedis.commands.ServerCommands",
                                                                                "redis.clients.jedis.commands.DatabaseCommands",
                                                                                "redis.clients.jedis.commands.KeyCommands",
                                                                                "redis.clients.jedis.commands.StringCommands",
                                                                                "redis.clients.jedis.commands.ListCommands",
                                                                                "redis.clients.jedis.commands.HashCommands",
                                                                                "redis.clients.jedis.commands.SetCommands",
                                                                                "redis.clients.jedis.commands.SortedSetCommands",
                                                                                "redis.clients.jedis.commands.GeoCommands",
                                                                                "redis.clients.jedis.commands.HyperLogLogCommands",
                                                                                "redis.clients.jedis.commands.StreamCommands",
                                                                                "redis.clients.jedis.commands.ControlCommands",
                                                                                "redis.clients.jedis.commands.ControlBinaryCommands",
                                                                                "redis.clients.jedis.commands.ClusterCommands",
                                                                                "redis.clients.jedis.commands.ModuleCommands",
                                                                                "redis.clients.jedis.commands.GenericControlCommands",
                                                                                "redis.clients.jedis.commands.ConfigCommands",
                                                                                "redis.clients.jedis.commands.ScriptingControlCommands",
                                                                                "redis.clients.jedis.commands.SlowlogCommands",
                                                                                "redis.clients.jedis.commands.ScriptingKeyCommands"
                                                                            ))))
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.OnCommand")
                ),

            forClass("redis.clients.jedis.BinaryJedis")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(ElementMatchers.isPublic()
                                                                            .and(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                                                                            .and(ElementMatchers.isOverriddenFrom(ElementMatchers.namedOneOf(
                                                                                // 2.9
                                                                                "redis.clients.jedis.BasicCommands",
                                                                                "redis.clients.jedis.BinaryJedisCommands",
                                                                                "redis.clients.jedis.MultiKeyBinaryCommands",
                                                                                "redis.clients.jedis.AdvancedBinaryJedisCommands",
                                                                                "redis.clients.jedis.BinaryScriptingCommands",

                                                                                // 3.x
                                                                                "redis.clients.jedis.commands.BasicCommands",
                                                                                "redis.clients.jedis.commands.BinaryJedisCommands",
                                                                                "redis.clients.jedis.commands.MultiKeyBinaryCommands",
                                                                                "redis.clients.jedis.commands.AdvancedBinaryJedisCommands",
                                                                                "redis.clients.jedis.commands.BinaryScriptingCommands"
                                                                            ))))
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.OnCommand")
                ),

            forClass("redis.clients.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.OutputStream")))
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisOutputStream$Ctor")
                ),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.OutputStream")))
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisOutputStream$Ctor")
                ),

            //2.9.x
            forClass("redis.clients.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.InputStream")))
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisInputStream$Ctor")
                ),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.InputStream")))
                                                   .to("org.bithon.agent.plugin.jedis.interceptor.RedisInputStream$Ctor")
                )
        );
    }
}
