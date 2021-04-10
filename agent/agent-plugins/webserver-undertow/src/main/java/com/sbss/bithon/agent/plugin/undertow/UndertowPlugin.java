package com.sbss.bithon.agent.plugin.undertow;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class UndertowPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("io.undertow.Undertow")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("start")
                                                   .noArgs()
                                                   .to("com.sbss.bithon.agent.plugin.undertow.interceptor.UndertowStart")
                ),

            forClass("io.undertow.server.protocol.http.HttpOpenListener")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("setRootHandler")
                                                   .onArgs("io.undertow.server.HttpHandler")
                                                   .to("com.sbss.bithon.agent.plugin.undertow.interceptor.HttpOpenListenerSetRootHandler")
                ),

            forClass("io.undertow.server.HttpServerExchange")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("dispatch")
                                                   .onArgs("java.util.concurrent.Executor",
                                                           "io.undertow.server.HttpHandler")
                                                   .to("com.sbss.bithon.agent.plugin.undertow.interceptor.HttpServerExchangeDispatch")
                ),

            forClass("io.undertow.servlet.api.LoggingExceptionHandlerHandleThrowable")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("handleThrowable")
                                                   .onArgs("io.undertow.server.HttpServerExchange",
                                                           "javax.servlet.ServletRequest",
                                                           "javax.servlet.ServletResponse",
                                                           "java.lang.Throwable")
                                                   .to("com.sbss.bithon.agent.plugin.undertow.interceptor.LoggingExceptionHandlerHandleThrowable")
                )
        );
    }
}
