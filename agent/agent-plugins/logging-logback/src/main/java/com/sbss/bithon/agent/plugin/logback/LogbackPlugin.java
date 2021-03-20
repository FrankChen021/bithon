package com.sbss.bithon.agent.plugin.logback;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * logback pointcut and interceptor
 *
 * @author frankchen
 */
public class LogbackPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forClass("ch.qos.logback.classic.Logger")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("callAppenders",
                                                                    "ch.qos.logback.classic.spi.ILoggingEvent")
                                                   .to("com.sbss.bithon.agent.plugin.logback.interceptor.LoggerCallAppenders")
                )
        );
    }
}
