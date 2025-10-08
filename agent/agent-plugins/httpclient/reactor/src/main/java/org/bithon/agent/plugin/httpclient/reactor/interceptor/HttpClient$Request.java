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

package org.bithon.agent.plugin.httpclient.reactor.interceptor;


import io.netty.handler.codec.http.HttpMethod;
import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.httpclient.reactor.HttpClientContext;

/**
 * {@link reactor.netty.http.client.HttpClient#request(HttpMethod)}
 *
 * @author frank.chen021@outlook.com
 * @date 1/10/25 6:16 pm
 */
public class HttpClient$Request extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        // A new HttpClientFinalizer is returned by this method
        Object returning = aopContext.getReturningAs();
        if (!(returning instanceof IBithonObject)) {
            return;
        }
        IBithonObject returningHttpClient = (IBithonObject) returning;

        HttpMethod method = aopContext.getArgAs(0);

        IBithonObject currentHttpClient = aopContext.getTargetAs();
        Object currentContext = currentHttpClient.getInjectedObject();
        if (currentContext != null) {
            returningHttpClient.setInjectedObject(((HttpClientContext) currentContext).withMethod(method.name()));
        } else {
            HttpClientContext context = new HttpClientContext();
            context.setMethod(method.name());
            returningHttpClient.setInjectedObject(context);
        }
    }
}
