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

package org.bithon.agent.plugin.spring.webflux.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;
import org.bithon.agent.plugin.spring.webflux.context.HttpServerContext;

/**
 * Inject an object to {@link reactor.netty.http.server.HttpServerOperations} to make the code in HttpBodySizeCollector a little easier
 *
 * @author frank.chen021@outlook.com
 * @date 6/11/21 9:05 pm
 */
public class HttpServerOperations$Ctor extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        IBithonObject obj = (IBithonObject) aopContext.getTarget();

        // Since this interceptor hooks on all ctors, check if the injected object is assigned first
        if (obj.getInjectedObject() == null) {
            obj.setInjectedObject(new HttpServerContext());
        }
    }
}
