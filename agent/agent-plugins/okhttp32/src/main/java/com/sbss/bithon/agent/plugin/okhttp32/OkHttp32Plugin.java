package com.sbss.bithon.agent.plugin.okhttp32;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * OkHttp3, for version < 3.2
 *
 * @author frankchen
 */
public class OkHttp32Plugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("okhttp3.RealCall")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("getResponseWithInterceptorChain",
                                         "boolean")
                        .to("com.sbss.bithon.agent.plugin.okhttp32.OkHttp32Interceptor")
                )

            /*
            forClass("okhttp3.internal.http.BridgeInterceptor")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("intercept")
                        .to("com.sbss.commons.agent.plugin.okhttp32.OkHttp32TraceInterceptorHandler")
                ),

            forClass("okhttp3.Request$Builder")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("build")
                        .to("com.sbss.commons.agent.plugin.okhttp32.OkHttp32TraceRequestHandler")
                )
             */
        );
    }
}
