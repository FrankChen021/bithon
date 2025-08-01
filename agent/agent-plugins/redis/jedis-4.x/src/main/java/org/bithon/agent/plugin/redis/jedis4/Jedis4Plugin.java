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
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * {@link redis.clients.jedis.Jedis}
 *
 * @author frankchen
 */
public class Jedis4Plugin implements IPlugin {

    @Override
    public IInterceptorPrecondition getPreconditions() {
        return
            IInterceptorPrecondition.and(
                // Both 3.x and 4.x has this class
                IInterceptorPrecondition.isClassDefined("redis.clients.jedis.Connection"),
                // 4.x does not have this class
                IInterceptorPrecondition.not(IInterceptorPrecondition.isClassDefined("redis.clients.jedis.Client")));
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(

            forClass("redis.clients.jedis.Connection")
                .onMethod("connect")
                .andNoArgs()
                .interceptedBy("org.bithon.agent.plugin.redis.jedis4.interceptor.Connection$Connect")
                .build(),

            forClass("redis.clients.jedis.Jedis")
                .onMethod(Matchers.implement("redis.clients.jedis.commands.ServerCommands",
                                             "redis.clients.jedis.commands.DatabaseCommands",
                                             "redis.clients.jedis.commands.JedisCommands",
                                             "redis.clients.jedis.commands.JedisBinaryCommands",
                                             "redis.clients.jedis.commands.ControlCommands",
                                             "redis.clients.jedis.commands.ControlBinaryCommands",
                                             "redis.clients.jedis.commands.ClusterCommands",
                                             "redis.clients.jedis.commands.ModuleCommands",
                                             "redis.clients.jedis.commands.SentinelCommands"))
                .interceptedBy("org.bithon.agent.plugin.redis.jedis4.interceptor.OnCommand")
                .build(),

            forClass("redis.clients.jedis.util.RedisOutputStream")
                .onConstructor()
                .andArgsSize(2)
                .andArgs(0, "java.io.OutputStream")
                .interceptedBy("org.bithon.agent.plugin.redis.jedis4.interceptor.RedisOutputStream$Ctor")
                .build(),

            //3.x
            forClass("redis.clients.jedis.util.RedisInputStream")
                .onConstructor()
                .andArgsSize(2)
                .andArgs(0, "java.io.InputStream")
                .interceptedBy("org.bithon.agent.plugin.redis.jedis4.interceptor.RedisInputStream$Ctor")
                .build()
        );
    }
}
