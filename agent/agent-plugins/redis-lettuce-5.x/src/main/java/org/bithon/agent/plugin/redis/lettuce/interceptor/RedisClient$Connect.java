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
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.utils.HostAndPort;
import org.bithon.component.commons.utils.ReflectionUtils;


/**
 * @author frankchen
 */
public class RedisClient$Connect extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        Object connection = aopContext.getReturning();
        if (connection instanceof IBithonObject) {
            // forward endpoint to connection
            RedisURI uri = ((RedisURI) ReflectionUtils.getFieldValue(aopContext.getTarget(), "redisURI"));

            // Since RedisClient allows passing RedisURI to connect method
            // it's a little bit complex to intercept this method to keep HostAndPort on RedisClient
            // So, we always construct a HostAndPort string here
            ((IBithonObject) connection).setInjectedObject(HostAndPort.of(uri.getHost(), uri.getPort()));
        }
    }
}
