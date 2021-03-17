package com.sbss.bithon.agent.plugin.tomcat;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class TomcatPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            //web server
            forClass("org.apache.tomcat.util.net.AbstractEndpoint")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("start")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.AbstractEndpointStart")
                ),

            // statistics
            // differ from Trace below since it depends on different response object
            forClass("org.apache.catalina.connector.CoyoteAdapter")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("service")
                                                   .onArgs("org.apache.coyote.Request", "org.apache.coyote.Response")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.CoyoteAdapterService")
                ),

            //exception
            forClass("org.apache.catalina.core.StandardWrapperValve")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("exception")
                                                   .onArgs("org.apache.catalina.connector.Request",
                                                           "org.apache.catalina.connector.Response",
                                                           "java.lang.Throwable")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.StandardWrapperValveException")
                ),

            //trace
            forClass("org.apache.catalina.core.StandardHostValve")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onAllMethods("invoke")
                                                   .onArgs("org.apache.catalina.connector.Request",
                                                           "org.apache.catalina.connector.Response")
                                                   .to("com.sbss.bithon.agent.plugin.tomcat.interceptor.StandardHostValveInvoke")
                )
        );
    }
}
