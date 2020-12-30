package com.sbss.bithon.agent.plugin.jetty;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class JettyPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.eclipse.jetty.server.AbstractConnector")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("doStart")
                        .to("com.sbss.bithon.agent.plugin.jetty.ConnectorStartInterceptor")
                ),

            forClass("org.eclipse.jetty.util.thread.QueuedThreadPool")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("doStart")
                        .to("com.sbss.bithon.agent.plugin.jetty.ThreadPoolHandler")
                ),

            forClass("org.eclipse.jetty.server.handler.ContextHandler")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndArgs("doHandle",
                                         "java.lang.String",
                                         "org.eclipse.jetty.server.Request",
                                         "javax.servlet.http.HttpServletRequest",
                                         "javax.servlet.http.HttpServletResponse")
                        .to("com.sbss.bithon.agent.plugin.jetty.HandleRequestInterceptor")
                )
        );
    }
}
