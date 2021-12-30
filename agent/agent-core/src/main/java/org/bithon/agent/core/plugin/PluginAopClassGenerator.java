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

package org.bithon.agent.core.plugin;


import org.bithon.agent.bootstrap.aop.BootstrapConstructorAop;
import org.bithon.agent.bootstrap.aop.BootstrapMethodAop;
import org.bithon.agent.core.aop.AopClassHelper;
import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptor;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For the instrumentation of bootstrap classes, an AOP class for each interceptor is generated to perform the interception
 *
 * @author frankchen
 * @date 2021-02-18 19:23
 */
public class PluginAopClassGenerator {

    private final Map<String, byte[]> classesTypeMap = new HashMap<>();
    private final Instrumentation instrumentation;
    private final AgentBuilder agentBuilder;

    public PluginAopClassGenerator(Instrumentation instrumentation,
                                   AgentBuilder agentBuilder) {
        this.instrumentation = instrumentation;
        this.agentBuilder = agentBuilder;
    }

    public static String bootstrapAopClass(String methodsInterceptor) {
        return methodsInterceptor + "Aop";
    }

    public AgentBuilder generate(List<IPlugin> plugins) {
        for (IPlugin plugin : plugins) {
            generateAop4Plugin(plugin);
        }
        return injectClassToClassLoader();
    }

    private void generateAop4Plugin(IPlugin plugin) {
        for (InterceptorDescriptor interceptor : plugin.getInterceptors()) {
            if (!interceptor.isBootstrapClass()) {
                continue;
            }
            for (MethodPointCutDescriptor pointCut : interceptor.getMethodPointCutDescriptors()) {
                generateAopClass(pointCut.getInterceptor(),
                                 pointCut);
            }
        }
    }

    private AgentBuilder injectClassToClassLoader() {
        if (!classesTypeMap.isEmpty()) {
            return agentBuilder.with(new AgentBuilder.InjectionStrategy.UsingUnsafe.OfFactory(AopClassHelper.inject(classesTypeMap)));
        } else {
            return agentBuilder;
        }
    }

    private void generateAopClass(String interceptorClass,
                                  MethodPointCutDescriptor methodPointCutDescriptor) {
        switch (methodPointCutDescriptor.getTargetMethodType()) {
            case NON_CONSTRUCTOR:
                generateAopClass(classesTypeMap,
                                 BootstrapMethodAop.class,
                                 interceptorClass,
                                 methodPointCutDescriptor);
                break;

            case CONSTRUCTOR:
                generateAopClass(classesTypeMap,
                                 BootstrapConstructorAop.class,
                                 interceptorClass,
                                 methodPointCutDescriptor);
                break;

            default:
                break;
        }
    }

    private void generateAopClass(Map<String, byte[]> classesTypeMap,
                                  Class<?> baseBootstrapAopClass,
                                  String interceptorClass,
                                  MethodPointCutDescriptor methodPointCutDescriptor) {
        String targetAopClassName = bootstrapAopClass(interceptorClass);

        byte[] aopClassBytes = AopClassHelper.generateAopClass(baseBootstrapAopClass,
                                                               targetAopClassName,
                                                               interceptorClass,
                                                               methodPointCutDescriptor.isDebug()).getBytes();

        classesTypeMap.put(targetAopClassName, aopClassBytes);
    }
}
