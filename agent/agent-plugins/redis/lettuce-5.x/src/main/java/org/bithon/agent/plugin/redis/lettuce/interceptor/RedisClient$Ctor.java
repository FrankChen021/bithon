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

import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.ClientResources;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.utils.HostAndPort;

/**
 * hook on {@link io.lettuce.core.RedisClient#RedisClient(ClientResources, RedisURI)}
 * See {@link LettuceConnection$Commands} to know more about how context object are passed
 */
public class RedisClient$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        RedisURI uri = aopContext.getArgAs(1);
        if (uri != null) {
            ((IBithonObject) aopContext.getTarget()).setInjectedObject(HostAndPort.of(uri.getHost(), uri.getPort()));
        }
    }
}
