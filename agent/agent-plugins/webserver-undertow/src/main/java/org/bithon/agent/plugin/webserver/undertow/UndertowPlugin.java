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

package org.bithon.agent.plugin.webserver.undertow;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class UndertowPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("io.undertow.Undertow")
                .hook()
                .onMethodAndNoArgs("start")
                .to("org.bithon.agent.plugin.webserver.undertow.interceptor.UndertowStart")
                .build(),

            forClass("io.undertow.server.protocol.http.HttpOpenListener")
                .hook()
                .onMethodAndArgs("setRootHandler", "io.undertow.server.HttpHandler")
                .to("org.bithon.agent.plugin.webserver.undertow.interceptor.HttpOpenListenerSetRootHandler")
                .build(),

            forClass("io.undertow.server.HttpServerExchange")
                .hook()
                .onMethodAndArgs("dispatch",
                                 "java.util.concurrent.Executor",
                                 "io.undertow.server.HttpHandler")
                .to("org.bithon.agent.plugin.webserver.undertow.interceptor.HttpServerExchangeDispatch")
                .build(),

            forClass("io.undertow.servlet.api.LoggingExceptionHandler")
                .hook()
                .onMethodAndArgs("handleThrowable",
                                 "io.undertow.server.HttpServerExchange",
                                 "javax.servlet.ServletRequest",
                                 "javax.servlet.ServletResponse",
                                 "java.lang.Throwable")
                .to("org.bithon.agent.plugin.webserver.undertow.interceptor.LoggingExceptionHandler$HandleThrowable")
                .build()
        );
    }
}
