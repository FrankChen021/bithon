/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.lettuce.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.core.utils.HostAndPort;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import io.lettuce.core.RedisURI;


/**
 * @author frankchen
 */
public class RedisClientConnect extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
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
