/*
 *    Copyright 2020 bithon.org
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

package org.bithon.agent.plugin.logback;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

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
                .onMethodAndArgs("callAppenders",
                                 "ch.qos.logback.classic.spi.ILoggingEvent")
                .interceptedBy("org.bithon.agent.plugin.logback.interceptor.Logger$CallAppenders")
                .build(),

            forClass("ch.qos.logback.core.pattern.PatternLayoutBase")
                .onDefaultConstructor()
                .interceptedBy("org.bithon.agent.plugin.logback.interceptor.PatternLayout$Ctor")

                .onMethodAndArgs("setPattern", "java.lang.String")
                .interceptedBy("org.bithon.agent.plugin.logback.interceptor.PatternLayout$SetPattern")
                .build()
        );
    }
}
