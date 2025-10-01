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

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.plugin.httpclient.reactor.HttpClientContext;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * {@link reactor.netty.http.client.HttpClientFinalizer#uri(URI)}
 * {@link reactor.netty.http.client.HttpClientFinalizer#uri(String)}
 * {@link reactor.netty.http.client.HttpClientFinalizer#uri(Mono)} not supported
 * <p>
 * The uri method is a builder pattern that duplicates a new HttpClientFinalizer object
 * We also need to duplicate the context.
 *
 * @author frank.chen021@outlook.com
 * @date 6/9/24 3:58 pm
 */
public class HttpClientFinalizer$Uri extends AfterInterceptor {

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

        String uriText;
        Object uri = aopContext.getArgAs(0);
        if (uri instanceof String) {
            uriText = (String) uri;
        } else if (uri instanceof URI) {
            uriText = uri.toString();
        } else {
            return;
        }
        IBithonObject currentHttpClient = aopContext.getTargetAs();
        Object currentContext = currentHttpClient.getInjectedObject();

        if (currentContext != null) {
            returningHttpClient.setInjectedObject(((HttpClientContext) currentContext).withUri(uriText));
        } else {
            HttpClientContext context = new HttpClientContext();
            context.setUri(uriText);
            returningHttpClient.setInjectedObject(context);
        }
    }
}
