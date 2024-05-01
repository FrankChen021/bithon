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

package org.bithon.agent.plugin.redis.jedis4;

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
public class Jedis4Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        //not "redis.clients.jedis.Client"
        return IInterceptorPrecondition.hasClass("redis.clients.jedis.Connection");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Jedis")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.implement(
                                                       "redis.clients.jedis.commands.ServerCommands",
                                                       "redis.clients.jedis.commands.DatabaseCommands",

                                                       // JedisCommands
                                                       "redis.clients.jedis.commands.KeyCommands",
                                                       "redis.clients.jedis.commands.StringCommands",
                                                       "redis.clients.jedis.commands.ListCommands",
                                                       "redis.clients.jedis.commands.HashCommands",
                                                       "redis.clients.jedis.commands.SetCommands",
                                                       "redis.clients.jedis.commands.SortedSetCommands",
                                                       "redis.clients.jedis.commands.GeoCommands",
                                                       "redis.clients.jedis.commands.HyperLogLogCommands",
                                                       "redis.clients.jedis.commands.StreamCommands",
                                                       "redis.clients.jedis.commands.ScriptingKeyCommands",

                                                       "redis.clients.jedis.commands.ControlCommands",
                                                       "redis.clients.jedis.commands.ControlBinaryCommands",
                                                       "redis.clients.jedis.commands.ClusterCommands",
                                                       "redis.clients.jedis.commands.ModuleCommands",
                                                       "redis.clients.jedis.commands.GenericControlCommands",
                                                       "redis.clients.jedis.commands.SentinelCommands"
                                                       ))
                                                   .to("org.bithon.agent.plugin.redis.jedis4.interceptor.OnCommand")
                        ),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.OutputStream")))
                                                   .to("org.bithon.agent.plugin.redis.jedis4.interceptor.RedisOutputStream$Ctor")
                        ),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.InputStream")))
                                                   .to("org.bithon.agent.plugin.redis.jedis4.interceptor.RedisInputStream$Ctor")
                        )
                            );
    }
}
