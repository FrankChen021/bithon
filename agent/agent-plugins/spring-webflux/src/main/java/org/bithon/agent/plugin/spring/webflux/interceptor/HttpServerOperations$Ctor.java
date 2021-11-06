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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.plugin.spring.webflux.metric.HttpIOMetrics;

/**
 * Inject an object to HttpServerOperations to make the code in HttpBodySizeCollector a little easier
 * @author Frank Chen
 * @date 6/11/21 9:05 pm
 */
public class HttpServerOperations$Ctor extends AbstractInterceptor {

    @Override
    public void onConstruct(AopContext aopContext) {
        IBithonObject obj = (IBithonObject) aopContext.getTarget();
        obj.setInjectedObject(new HttpIOMetrics());
    }
}
