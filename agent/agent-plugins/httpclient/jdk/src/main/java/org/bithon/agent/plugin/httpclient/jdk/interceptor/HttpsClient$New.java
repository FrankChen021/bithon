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
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import sun.net.www.protocol.http.HttpURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.net.Proxy;
import java.net.URL;

/**
 * {@link {@link sun.net.www.protocol.https.HttpsClient#New(SSLSocketFactory sf, URL url, HostnameVerifier hv, Proxy p, boolean useCache, int connectTimeout, HttpURLConnection httpuc)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/14 11:13 下午
 */
public class HttpsClient$New extends AfterInterceptor {

    /**
     * Inject HttpURLConnection instance, which creates HttpClient instance, into the instance of HttpClient as its parent
     *
     */
    @Override
    public void after(AopContext aopContext) {
        IBithonObject bithonObject = aopContext.getReturningAs();
        if (bithonObject == null) {
            // usually there's exception thrown when establish connection
            return;
        }

        java.net.HttpURLConnection urlConnection = aopContext.getArgAs(6);
        if (bithonObject.getInjectedObject() == null) {
            bithonObject.setInjectedObject(new HttpClientContext());
        }

        HttpClientContext clientContext = (HttpClientContext) bithonObject.getInjectedObject();
        clientContext.setMethod(urlConnection.getRequestMethod());
        clientContext.setUrl(urlConnection.getURL().toString());
    }
}
