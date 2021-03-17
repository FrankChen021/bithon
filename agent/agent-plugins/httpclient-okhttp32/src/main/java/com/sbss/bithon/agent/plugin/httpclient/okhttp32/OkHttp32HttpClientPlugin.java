package com.sbss.bithon.agent.plugin.httpclient.okhttp32;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * OkHttp3, for version < 3.2
 *
 * @author frankchen
 */
public class OkHttp32HttpClientPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Collections.singletonList(
            forClass("okhttp3.RealCall")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("getResponseWithInterceptorChain",
                                         "boolean")
                        .to("com.sbss.bithon.agent.plugin.httpclient.okhttp32.OkHttp32Interceptor")
                )

            /*
            forClass("okhttp3.internal.http.BridgeInterceptor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("intercept")
                        .to("com.sbss.commons.agent.plugin.httpclient.okhttp32.OkHttp32TraceInterceptorHandler")
                ),

            forClass("okhttp3.Request$Builder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("build")
                        .to("com.sbss.commons.agent.plugin.httpclient.okhttp32.OkHttp32TraceRequestHandler")
                )
             */
        );
    }
}
