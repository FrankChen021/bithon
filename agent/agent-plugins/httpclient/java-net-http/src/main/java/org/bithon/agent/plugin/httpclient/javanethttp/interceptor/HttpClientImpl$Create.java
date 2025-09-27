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

package org.bithon.agent.plugin.httpclient.javanethttp.interceptor;

import jdk.internal.net.http.HttpClientBuilderImpl;
import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.java.adaptor.JavaAdaptorFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.net.http.HttpClient;

/**
 * {@link jdk.internal.net.http.HttpClientImpl#create(HttpClientBuilderImpl)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class HttpClientImpl$Create extends BeforeInterceptor {
    static {
        // The thread pool needs to access jdk.internal.net.http.HttpClientImpl$DefaultThreadFactory.namePrefix
        // It's the ReflectionUtils that access this field,
        // so we need to make sure that the module jdk.internal.net.http is opened to the module of that class
        JavaAdaptorFactory.getAdaptor()
                          .openPackages(InstrumentationHelper.getInstance(),
                                        HttpClient.class,
                                        "jdk.internal.net.http",
                                        // We think that the ThreadPool interceptor is in the same module of this interceptor
                                        ReflectionUtils.class);
    }
}
