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

package org.bithon.agent.plugin.tomcat.interceptor;

import org.apache.catalina.core.StandardContext;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.instrumentation.loader.AgentClassLoader;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Hook on {@link StandardContext#StandardContext()} to register sentinel filter
 *
 * @author frankchen
 */
public class StandardContext$Ctor extends AfterInterceptor {

    /**
     * Call {@link StandardContext#addServletContainerInitializer(javax.servlet.ServletContainerInitializer, Set)} to hook the sentinel filter
     */
    @Override
    public void after(AopContext aopContext) {
        StandardContext standardContext = aopContext.getTargetAs();

        //
        // This plugin is shared for tomcat 8 ~ 10
        // For tomcat 10 which is used by SpringBoot3, it uses jakarta servlet instead of javax servlet which is used by tomcat 8 and 9.
        // To share the code base, we can't invoke the method directly because this plugin is complied by tomcat 8 signature.
        // So we need reflection to detect which method should be invoked.
        //
        String sentinelFilterClass;

        Optional<Method> addServletContainerInitializer = Arrays.stream(standardContext.getClass()
                                                                                       .getMethods())
                                                                .filter(m -> "addServletContainerInitializer".equals(m.getName()))
                                                                .findFirst();
        if (!addServletContainerInitializer.isPresent()) {
            return;
        }

        String initializerType = addServletContainerInitializer.get().getParameterTypes()[0].getName();
        if (initializerType.startsWith("jakarta.")) {
            sentinelFilterClass = "org.bithon.agent.sentinel.servlet.filter.jakarta.SentinelInitializer";
        } else if (initializerType.startsWith("javax.")) {
            sentinelFilterClass = "org.bithon.agent.sentinel.servlet.filter.javax.SentinelInitializer";
        } else {
            return;
        }
        try {
            Class sentinelClazz = Class.forName(sentinelFilterClass, true, AgentClassLoader.getClassLoader());
            Object initializer = sentinelClazz.getConstructor().newInstance();

            addServletContainerInitializer.get().invoke(standardContext, initializer, Collections.emptySet());
        } catch (Exception e) {
            LoggerFactory.getLogger(StandardContext$Ctor.class).error("Failed to set up sentinel filter to current tomcat webserver", e);
        }
    }
}
