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

package org.bithon.agent.plugin.mongodb.interceptor;

import com.mongodb.connection.ServerId;
import com.mongodb.connection.StreamFactory;
import com.mongodb.event.ConnectionListener;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/30 11:29 上午
 */
public class InternalStreamConnection {

    /**
     * {@link com.mongodb.connection.InternalStreamConnection#InternalStreamConnection(ServerId, StreamFactory, com.mongodb.connection.InternalConnectionInitializer, ConnectionListener)}
     */
    public static class Constructor extends AbstractInterceptor {
        @Override
        public void onConstruct(AopContext aopContext) {
            IBithonObject bithonObject = aopContext.castTargetAs();
            bithonObject.setInjectedObject(((ServerId) aopContext.getArgAs(0)).getAddress().toString());
        }
    }
}
