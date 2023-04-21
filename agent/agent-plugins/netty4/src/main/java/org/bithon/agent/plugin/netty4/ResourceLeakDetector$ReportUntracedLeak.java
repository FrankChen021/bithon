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

package org.bithon.agent.plugin.netty4;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.event.ExceptionBuilder;
import org.bithon.agent.observability.event.ExceptionCollector;
import org.bithon.component.commons.utils.StringUtils;

/**
 * interceptor of {@link io.netty.util.ResourceLeakDetector#reportUntracedLeak(String)}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/5/13 5:32 下午
 */
public class ResourceLeakDetector$ReportUntracedLeak extends BeforeInterceptor {

    @Override
    public void before(AopContext aopContext) {
        ExceptionBuilder builder = ExceptionBuilder.builder()
                                                   .message(StringUtils.format("LEAK: %s.release() was not called before it's garbage-collected.", aopContext.getArgs()[0]))
                                                   .exceptionClass(aopContext.getTargetClass());
        ExceptionCollector.collect(builder);
    }
}
