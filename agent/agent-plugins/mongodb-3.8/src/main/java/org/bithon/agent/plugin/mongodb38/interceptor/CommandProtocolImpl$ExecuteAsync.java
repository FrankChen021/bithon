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

package org.bithon.agent.plugin.mongodb38.interceptor;

import com.mongodb.async.SingleResultCallback;
import com.mongodb.internal.connection.InternalConnection;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;

/**
 * TODO:
 * {@link com.mongodb.internal.connection.CommandProtocolImpl#executeAsync(InternalConnection, SingleResultCallback)}
 */
public class CommandProtocolImpl$ExecuteAsync extends AbstractInterceptor {
    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {
        super.onMethodLeave(aopContext);
    }
}
