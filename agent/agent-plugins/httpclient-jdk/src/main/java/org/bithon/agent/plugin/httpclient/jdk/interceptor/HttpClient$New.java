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

import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.context.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.AfterInterceptor;
import sun.net.www.protocol.http.HttpURLConnection;

import java.net.Proxy;
import java.net.URL;

/**
 * {@link sun.net.www.http.HttpClient#New(URL, Proxy, int, boolean, HttpURLConnection)}
 * <p>
 * NOTE: this method is returned after doConnect is called
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 11:13 下午
 */
public class HttpClient$New extends AfterInterceptor {
    /**
     * inject HttpURLConnection instance, which creates HttpClient instance, into the instance of HttpClient as its parent
     */
    @Override
    public void after(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getReturningAs();
        if (bithonObject == null) {
            // usually there's exception thrown when establish connection
            return;
        }

        java.net.HttpURLConnection urlConnection = aopContext.getArgAs(4);
        if (bithonObject.getInjectedObject() == null) {
            bithonObject.setInjectedObject(new HttpClientContext());
        }

        HttpClientContext clientContext = (HttpClientContext) bithonObject.getInjectedObject();
        clientContext.setMethod(urlConnection.getRequestMethod());
        clientContext.setUrl(urlConnection.getURL().toString());
    }
}
