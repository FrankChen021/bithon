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

package org.bithon.agent.plugin.log4j2;

import org.bithon.agent.core.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 */
public class Log4j2Plugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            /**
             * {@link org.apache.logging.log4j.core.Logger#logMessage(String, org.apache.logging.log4j.Level, org.apache.logging.log4j.Marker, String, Throwable)}
             * {@link org.apache.logging.log4j.core.Logger#logMessage(String, org.apache.logging.log4j.Level, org.apache.logging.log4j.Marker, Object, Throwable)}
             * {@link org.apache.logging.log4j.core.Logger#logMessage(String, org.apache.logging.log4j.Level, org.apache.logging.log4j.Marker, CharSequence, Throwable)}
             */
            forClass("org.apache.logging.log4j.core.Logger")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("logMessage")
                                                                     .and(Matchers.takesArgument(1,
                                                                                                 "org.apache.logging.log4j.Level"))
                                                                     .and(Matchers.takesArgument(4,
                                                                                                 "java.lang.Throwable")))
                                                   .to("org.bithon.agent.plugin.log4j2.interceptor.Logger$LogMessage")
                ),

            forClass("org.apache.logging.log4j.core.pattern.PatternParser")
                .methods(
                    /**
                     * {@link org.apache.logging.log4j.core.pattern.PatternParser#PatternParser(org.apache.logging.log4j.core.config.Configuration, String, Class, Class)}
                     */
                    MethodPointCutDescriptorBuilder.build()
                                                   .onConstructor(Matchers.takesArguments(4))
                                                   .to("org.bithon.agent.plugin.log4j2.interceptor.PatternParser$Ctor"),

                    /**
                     * {@link org.apache.logging.log4j.core.pattern.PatternParser#parse(String, List, List, boolean, boolean, boolean)}
                     */
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(Matchers.withName("parse")
                                                                     .and(Matchers.takesArgument(0,
                                                                                                 "java.lang.String"))
                                                                     .and(Matchers.takesArguments(6)))
                                                   .to("org.bithon.agent.plugin.log4j2.interceptor.PatternParser$Parse")
                )
        );
    }
}
