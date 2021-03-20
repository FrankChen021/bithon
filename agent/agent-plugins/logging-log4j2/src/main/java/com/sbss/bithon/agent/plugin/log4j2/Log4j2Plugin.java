package com.sbss.bithon.agent.plugin.log4j2;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MatcherUtils;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author frankchen
 */
public class Log4j2Plugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.singletonList(
            forClass("org.apache.logging.log4j.core.Logger")
                .methods(
                    /*
                     * java.lang.String
                     * org.apache.logging.log4j.Level
                     * org.apache.logging.log4j.Marker
                     * org.apache.logging.log4j.message.Message
                     * java.lang.Throwable"
                     */
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(named("logMessage")
                                                                 .and(MatcherUtils.takesArgument(1,
                                                                                                 "org.apache.logging.log4j.Level"))
                                                                 .and(MatcherUtils.takesArgument(4,
                                                                                                 "java.lang.Throwable")))
                                                   .to("com.sbss.bithon.agent.plugin.log4j2.interceptor.LoggerLogMessage")
                )
        );
    }
}
