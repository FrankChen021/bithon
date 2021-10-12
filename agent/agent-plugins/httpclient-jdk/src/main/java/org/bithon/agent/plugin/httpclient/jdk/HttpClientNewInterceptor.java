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

package org.bithon.agent.plugin.httpclient.jdk;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import sun.net.www.protocol.http.HttpURLConnection;

import java.net.Proxy;
import java.net.URL;

/**
 * {@link sun.net.www.http.HttpClient#New(URL, Proxy, int, boolean, HttpURLConnection)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 11:13 下午
 */
public class HttpClientNewInterceptor extends AbstractInterceptor {
    /**
     * inject HttpURLConnection instance, which creates HttpClient instance, into the instance of HttpClient as its parent
     *
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        IBithonObject injectedObject = aopContext.castReturningAs();
        if (injectedObject == null) {
            // usually there's exception thrown when establish connection
            return;
        }

        injectedObject.setInjectedObject(aopContext.getArgs()[4]);
    }
}
