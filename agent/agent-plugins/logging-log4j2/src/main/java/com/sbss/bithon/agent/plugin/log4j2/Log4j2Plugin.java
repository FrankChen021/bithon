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
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;

import java.util.Collections;
import java.util.List;

import static com.sbss.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;
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
                    /**
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
