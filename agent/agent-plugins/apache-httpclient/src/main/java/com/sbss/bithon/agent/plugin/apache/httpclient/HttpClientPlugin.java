package com.sbss.bithon.agent.plugin.apache.httpclient;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class HttpClientPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            //
            // metrics
            //
            forClass("org.apache.http.impl.client.InternalHttpClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("doExecute",
                                         "org.apache.http.HttpHost",
                                         "org.apache.http.HttpRequest",
                                         "org.apache.http.protocol.HttpContext")
                        .to("com.sbss.bithon.agent.plugin.apache.httpclient.metrics.HttpClientExecuteInterceptor")
                    ),

            forClass("org.apache.http.impl.execchain.RedirectExec")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("execute",
                                         "org.apache.http.conn.routing.HttpRoute",
                                         "org.apache.http.client.methods.HttpRequestWrapper",
                                         "org.apache.http.client.protocol.HttpClientContext",
                                         "org.apache.http.client.methods.HttpExecutionAware")
                        .to("com.sbss.bithon.agent.plugin.apache.httpclient.metrics.HttpClientExecuteInterceptor")
                ),

            forClass("org.apache.http.impl.execchain.MinimalClientExec")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("execute",
                                         "org.apache.http.conn.routing.HttpRoute",
                                         "org.apache.http.client.methods.HttpRequestWrapper",
                                         "org.apache.http.client.protocol.HttpClientContext",
                                         "org.apache.http.client.methods.HttpExecutionAware")
                        .to("com.sbss.bithon.agent.plugin.apache.httpclient.metrics.HttpClientExecuteInterceptor")
                ),

            forClass("org.apache.http.impl.client.DefaultRequestDirector")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("execute",
                                         "org.apache.http.HttpHost",
                                         "org.apache.http.HttpRequest",
                                         "org.apache.http.protocol.HttpContext")
                        .to("com.sbss.bithon.agent.plugin.apache.httpclient.metrics.DefaultRequestDirectorExecute"),

                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("releaseConnection")
                        .to("com.sbss.bithon.agent.plugin.apache.httpclient.metrics.DefaultRequestDirectorReleaseConnection")
                ),

            //
            // tracing
            //
            forClass("org.apache.http.protocol.HttpRequestExecutor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("execute",
                                         "org.apache.http.HttpRequest",
                                         "org.apache.http.HttpClientConnection",
                                         "org.apache.http.protocol.HttpContext")
                        .to("com.sbss.bithon.agent.plugin.apache.httpclient.trace.HttpRequestInterceptor")
                )
        );
    }
}