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
        return IInterceptorPrecondition.isClassDefined("redis.clients.jedis.ClusterReset");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Jedis")
                .onMethod(Matchers.implement(
                    "redis.clients.jedis.commands.JedisCommands",
                    "redis.clients.jedis.commands.MultiKeyCommands",
                    "redis.clients.jedis.commands.AdvancedJedisCommands",
                    "redis.clients.jedis.commands.ScriptingCommands",
                    "redis.clients.jedis.commands.BasicCommands",
                    "redis.clients.jedis.commands.ClusterCommands",
                    "redis.clients.jedis.commands.SentinelCommands",
                    "redis.clients.jedis.commands.ModuleCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.jedis3.interceptor.OnCommand")
                .build(),

            forClass("redis.clients.jedis.BinaryJedis")
                .onMethod(Matchers.implement(
                    "redis.clients.jedis.commands.BasicCommands",
                    "redis.clients.jedis.commands.BinaryJedisCommands",
                    "redis.clients.jedis.commands.MultiKeyBinaryCommands",
                    "redis.clients.jedis.commands.AdvancedBinaryJedisCommands",
                    "redis.clients.jedis.commands.BinaryScriptingCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.jedis3.interceptor.OnCommand")
                .build(),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .onConstructor()
                .andArgsSize(2)
                .andArgs(0, "java.io.OutputStream")
                .interceptedBy("org.bithon.agent.plugin.redis.jedis3.interceptor.RedisOutputStream$Ctor")
                .build(),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .onConstructor()
                .andArgsSize(2)
                .andArgs(0, "java.io.InputStream")
                .interceptedBy("org.bithon.agent.plugin.redis.jedis3.interceptor.RedisInputStream$Ctor")
                .build()
        );
    }
}
