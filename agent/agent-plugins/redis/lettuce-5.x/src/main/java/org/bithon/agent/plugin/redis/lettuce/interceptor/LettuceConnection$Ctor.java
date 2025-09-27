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
 * Hook on {@link org.springframework.data.redis.connection.lettuce.LettuceConnection} ctor
 * to copy context information from given connection to this LettuceConnection
 *
 * @author frank.chen021@outlook.com
 * @date 1/5/24 11:12 am
 */
public class LettuceConnection$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        Object connection = aopContext.getArgAs(0);
        if (!(connection instanceof IBithonObject)) {
            // The connection is type of StatefulConnection which has implementation of classes defined in LettucePlugin#getBithonClassDescriptor
            // These connections are connected via RedisClient$Connect
            return;
        }

        // The endpoint is injected in RedisClient$Connect
        String endpoint = (String) ((IBithonObject) connection).getInjectedObject();
        int dbIndex = aopContext.getArgAs(3);

        // Set the connection context info to the target object, so when commands object are returned(see LettuceConnection#Command), the context info can be got
        ((IBithonObject) aopContext.getTarget()).setInjectedObject(new ConnectionContext(endpoint, dbIndex));
    }
}
