package com.sbss.bithon.agent.plugin.httpclient.okhttp3;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * OkHttp3 plugin, for version > 3.3
 *
 * @author frankchen
 */
public class OkHttp3HttpClientPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forClass("okhttp3.RealCall")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndNoArgs("getResponseWithInterceptorChain")
                                                   .to("com.sbss.bithon.agent.plugin.httpclient.okhttp3.OkHttp3Interceptor")
                )
        );
    }
}
