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

package org.bithon.agent.plugin.redis.jedis2;

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
public class Jedis2Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        // 2.x specified class
        return IInterceptorPrecondition.isClassDefined("redis.clients.util.RedisInputStream");
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(

            forClass("redis.clients.jedis.Jedis")
                .onMethod(Matchers.implement(
                    "redis.clients.jedis.JedisCommands",
                    "redis.clients.jedis.MultiKeyCommands",
                    "redis.clients.jedis.AdvancedJedisCommands",
                    "redis.clients.jedis.ScriptingCommands",
                    "redis.clients.jedis.BasicCommands",
                    "redis.clients.jedis.ClusterCommands",
                    "redis.clients.jedis.SentinelCommands"))
                .to("org.bithon.agent.plugin.redis.jedis2.interceptor.OnCommand")
                .build(),

            forClass("redis.clients.jedis.BinaryJedis")
                .onMethod(Matchers.implement("redis.clients.jedis.BasicCommands",
                                             "redis.clients.jedis.BinaryJedisCommands",
                                             "redis.clients.jedis.MultiKeyBinaryCommands",
                                             "redis.clients.jedis.AdvancedBinaryJedisCommands",
                                             "redis.clients.jedis.BinaryScriptingCommands"))
                .to("org.bithon.agent.plugin.redis.jedis2.interceptor.OnCommand")
                .build(),

            forClass("redis.clients.util.RedisOutputStream")
                .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.OutputStream")))
                .to("org.bithon.agent.plugin.redis.jedis2.interceptor.RedisOutputStream$Ctor")
                .build(),

            //2.9.x
            forClass("redis.clients.util.RedisInputStream")
                .onConstructor(Matchers.takesArguments(2).and(Matchers.takesFirstArgument("java.io.InputStream")))
                .to("org.bithon.agent.plugin.redis.jedis2.interceptor.RedisInputStream$Ctor")
                .build()
        );
    }
}
