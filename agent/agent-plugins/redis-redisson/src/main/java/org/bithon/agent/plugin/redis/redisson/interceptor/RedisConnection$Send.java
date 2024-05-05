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

package org.bithon.agent.plugin.redis.redisson.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.RedisCommand;

/**
 * {@link org.redisson.client.RedisConnection#send(CommandData)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/5 10:30
 */
public class RedisConnection$Send extends BeforeInterceptor {

    @Override
    public void before(AopContext aopContext) {
        CommandData<?, ?> commandData = aopContext.getArgAs(0);

        RedisCommand<?> redisCommand = commandData.getCommand();
        CommandContext commandContext = (CommandContext) ((IBithonObject) redisCommand).getInjectedObject();
        if (commandContext != null) {
            commandContext.begin(redisCommand.getName());
        }
    }
}
