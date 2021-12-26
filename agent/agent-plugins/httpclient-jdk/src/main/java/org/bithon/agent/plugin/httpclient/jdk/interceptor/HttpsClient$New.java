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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/14 11:13 下午
 */
public class HttpsClient$New extends AbstractInterceptor {
    /**
     * inject HttpURLConnection instance, which creates HttpClient instance, into the instance of HttpClient as its parent
     *
     * @param aopContext
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        IBithonObject injectedObject = aopContext.castReturningAs();
        if (injectedObject == null) {
            // usually there's exception thrown when establish connection
            return;
        }

        injectedObject.setInjectedObject(aopContext.getArgs()[6]);
    }
}
