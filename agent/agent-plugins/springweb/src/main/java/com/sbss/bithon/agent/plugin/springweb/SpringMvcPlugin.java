package com.sbss.bithon.agent.plugin.springweb;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class SpringMvcPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("org.springframework.web.servlet.handler.AbstractHandlerMethodMapping")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("registerHandlerMethod",
                                         "java.lang.Object", "java.lang.reflect.Method", "T")
                        .to("com.sbss.bithon.agent.plugin.springweb.MethodMatchingInterceptor")
                ),

            forClass("org.springframework.web.client.RestTemplate")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onAllMethods("execute")
                        .to("com.sbss.bithon.agent.plugin.springweb.RestTemplateExecuteInterceptor")
                ),

            // Request.create
            forClass("org.springframework.cloud.netflix.feign.ribbon.LoadBalancerFeignClient")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("execute",
                                         "feign.Request", "feign.Request$Options")
                        .to("com.sbss.bithon.agent.plugin.springweb.FeignClientInterceptor")
                )
        );
    }
}
