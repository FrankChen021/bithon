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

package com.sbss.bithon.agent.plugin.log4j2;

import com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.aop.descriptor.MatcherUtils;
import com.sbss.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import com.sbss.bithon.agent.core.plugin.IPlugin;

import java.util.Arrays;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;
import static shaded.net.bytebuddy.matcher.ElementMatchers.named;
import static shaded.net.bytebuddy.matcher.ElementMatchers.takesArguments;

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
                                                   .onMethod(named("logMessage")
                                                                 .and(MatcherUtils.takesArgument(1,
                                                                                                 "org.apache.logging.log4j.Level"))
                                                                 .and(MatcherUtils.takesArgument(4,
                                                                                                 "java.lang.Throwable")))
                                                   .to("com.sbss.bithon.agent.plugin.log4j2.interceptor.LoggerLogMessage")
                ),

            //org.apache.logging.log4j.core.pattern.PatternParser#parse(String, List, List, boolean, boolean, boolean)
            forClass("org.apache.logging.log4j.core.pattern.PatternParser")
                .methods(
                    MethodPointCutDescriptorBuilder.build()
                                                   .onMethod(named("parse")
                                                                 .and(MatcherUtils.takesArgument(0,
                                                                                                 "java.lang.String"))
                                                                 .and(takesArguments(6)))
                                                   .to("com.sbss.bithon.agent.plugin.log4j2.interceptor.PatternParserParse")
                )
        );
    }
}
