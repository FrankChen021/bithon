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

package org.bithon.agent.plugin.httpclient.jdk.interceptor;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;

/**
 * {@link sun.net.NetworkClient#doConnect(String, int)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/8/21 15:39
 */
public class NetworkClient$DoConnect extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            // NetworkClient has many subclasses, some of which are not instrumented
            return;
        }

        // doConnect is called inside construction of HttpClient object
        // we need to initialize context object here
        // but in case code change in JDK, we still check if context object is initialized
        IBithonObject bithonObject = (IBithonObject) aopContext.getTarget();
        if (bithonObject.getInjectedObject() == null) {
            bithonObject.setInjectedObject(new HttpClientContext());
        }

        final HttpClientContext clientContext = (HttpClientContext) bithonObject.getInjectedObject();

        //
        // doConnect returns an object of java.net.Socket
        // we need to inject the context to this object
        // So that corresponding interceptors of Socket can handle IO statistics
        //
        Object socket = aopContext.getReturning();
        if (socket instanceof IBithonObject) {
            ((IBithonObject) socket).setInjectedObject(clientContext);
        }
    }
}
