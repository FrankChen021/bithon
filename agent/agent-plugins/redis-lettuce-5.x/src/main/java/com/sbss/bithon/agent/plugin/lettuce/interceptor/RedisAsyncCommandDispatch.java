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
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import com.sbss.bithon.agent.plugin.lettuce.LettuceAsyncContext;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.api.StatefulConnection;

/**
 * @author frankchen
 */
public class RedisAsyncCommandDispatch extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (!(aopContext.getReturning() instanceof IBithonObject)) {
            return;
        }
        RedisAsyncCommandsImpl s;
        IBithonObject result = (IBithonObject) aopContext.getReturning();

        LettuceAsyncContext asyncContext = new LettuceAsyncContext();
        asyncContext.setStartTime(System.nanoTime());
        result.setInjectedObject(asyncContext);

        StatefulConnection<?, ?> connection = ((StatefulConnection<?, ?>) ReflectionUtils.getFieldValue(aopContext.getTarget(),
                                                                                                        "connection"));
        if (connection instanceof IBithonObject) {
            String endpoint = (String) ((IBithonObject) connection).getInjectedObject();
            asyncContext.setEndpoint(endpoint);
        }
    }
}
