package com.sbss.bithon.agent.plugin.quartz2;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptorBuilder;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class QuartzPlugin extends AbstractPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {

        return Arrays.asList(
            forClass("org.quartz.impl.SchedulerRepository")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onDefaultConstructor()
                        .to("com.sbss.bithon.agent.plugin.quartz2.QuartzInterceptor")
                ),

            forClass("org.quartz.core.JobRunShell")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onMethodAndNoArgs("run")
                        .to("com.sbss.bithon.agent.plugin.quartz2.QuartzJobExecutionLogInterceptor")
                )
        );
    }
}
