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
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.utils.ReflectionUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;

import java.util.function.Consumer;

/**
 * See {@link reactor.netty.http.client.HttpClientFinalizer#_connect()}
 * <p>
 * This class generates span log for http client connection initialization or acquiring before sending http request.
 * This helps us to get understood how long the gateway takes to establish/acquire a connection with the target service.
 *
 * @author frank.chen021@outlook.com
 * @date 6/9/24 3:58 pm
 */
public class HttpClientFinalizer$Connect extends BeforeInterceptor {

    @Override
    public void before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newAsyncSpan("http-client");
        if (span == null) {
            return;
        }

        HttpClient httpClient = aopContext.getTargetAs();
        HttpClientConfig httpClientConfig = httpClient.configuration();

        Consumer<HttpClientConfig> doOnConnect = (config) -> {
            span.method(HttpClient.class, "connect")
                .start();
        };
        if (httpClientConfig.doOnConnect() != null) {
            doOnConnect = doOnConnect.andThen(httpClientConfig.doOnConnect());
        }
        try {
            ReflectionUtils.setFieldValue(httpClientConfig, "doOnConnect", doOnConnect);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        // This is tricky.
        // On the HttpClient above, it provides doOnConnected callback,
        // however, it's triggered after the connection is turned into ACQUIRED if a connection is reused.
        // When an ACQUIRED state change is triggered, it will trigger to send the http request first.
        // See the MonoHttpConnect in the HttpClientConnect to know more about it.
        //
        // This is why we have to hook on the ConnectionObserver to get the connection state change before ACQUIRED state change notified.
        //
        // And we can't change the 'observer' field in HttpClientConfig directly,
        // because the 'observer' is used to compute a hash value to determine if a new connection pool is necessary.
        // So we have to hook on the 'defaultConnectionObserver' method to add our own observer.
        //
        // Once the instrumentation of HttpClientConfig is successfully installed,
        // it has the interface of IBithonObject
        //noinspection ConstantValue
        if ((Object) httpClientConfig instanceof IBithonObject) {
            ((IBithonObject) (Object) httpClientConfig).setInjectedObject(span);
        }
    }
}
