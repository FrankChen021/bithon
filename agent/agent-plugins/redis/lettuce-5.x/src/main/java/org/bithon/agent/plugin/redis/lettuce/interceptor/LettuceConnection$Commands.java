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

package org.bithon.agent.plugin.redis.lettuce.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

/**
 * Hook commands like {@link org.springframework.data.redis.connection.lettuce.LettuceConnection#listCommands()}
 * to get connection info for Commands.
 * <p> work flow:
 * 1. RedisClient#ctor --> endpoint
 * 2. RedisClient#connect --> set the endpoint to the returning connection object
 * 3. LettuceConnection#ctor --> get the endpoint from the passed connection object, set connection context
 * 4. LettuceConnection#commands --> set the connection context to the returning commands object
 *
 * @author frank.chen021@outlook.com
 * @date 1/5/24 11:12 am
 */
public class LettuceConnection$Commands extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        // The connection context is injected in LettuceConnection$Ctor when a LettuceConnection is created
        ConnectionContext connectionContext = aopContext.getInjectedOnTargetAs();
        if (connectionContext == null) {
            // this should not happen as it is injected in constructor
            return;
        }

        // The returning should be class like RedisListCommands, which has been instrumented by the plugin
        Object returningCommands = aopContext.getReturningAs();
        if (!(returningCommands instanceof IBithonObject)) {
            return;
        }

        ((IBithonObject) returningCommands).setInjectedObject(connectionContext);
    }
}
