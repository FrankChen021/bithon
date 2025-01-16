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

package org.bithon.agent.plugin.apache.zookeeper;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

/**
 * {@link org.apache.zookeeper.proto.RequestHeader}
 *
 * Inject IOContext into this object so that it can be accessed in other interceptors
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 10:33 pm
 */
public class RequestHeader$Ctor extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        IBithonObject requestHeader = aopContext.getTargetAs();
        if (requestHeader.getInjectedObject() == null) {
            requestHeader.setInjectedObject(new IOMetrics());
        }
    }
}
