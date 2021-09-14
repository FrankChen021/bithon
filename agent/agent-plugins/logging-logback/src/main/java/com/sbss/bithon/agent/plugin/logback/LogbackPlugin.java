/*
 *    Copyright 2020 bithon.cn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sbss.bithon.agent.plugin.logback;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * logback pointcut and interceptor
 *
 * @author frankchen
 */
public class LogbackPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("ch.qos.logback.classic.Logger")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("callAppenders",
                                                                    "ch.qos.logback.classic.spi.ILoggingEvent")
                                                   .to("com.sbss.bithon.agent.plugin.logback.interceptor.LoggerCallAppenders")
                ),

            forClass("ch.qos.logback.core.pattern.PatternLayoutBase")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                        .onDefaultConstructor()
                                                   .to("com.sbss.bithon.agent.plugin.logback.interceptor.PatternLayoutCtor"),

                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethodAndArgs("setPattern", "java.lang.String")
                                                   .to("com.sbss.bithon.agent.plugin.logback.interceptor.PatternLayoutSetPattern")
                )
        );
    }
}
