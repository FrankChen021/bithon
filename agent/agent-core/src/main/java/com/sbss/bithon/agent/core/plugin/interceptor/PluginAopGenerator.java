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

package com.sbss.bithon.agent.core.plugin.interceptor;


import com.sbss.bithon.agent.bootstrap.aop.BootstrapConstructorAop;
import com.sbss.bithon.agent.bootstrap.aop.BootstrapMethodAop;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.AopDebugger;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptor;
import shaded.net.bytebuddy.ByteBuddy;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * If there is Bootstrap instrumentation plugin declared in plugin list, BootstrapAopInstaller inject the necessary
 * classes into bootstrap class loader, including generated dynamic delegate classes.
 *
 * @author frankchen
 * @date 2021-02-18 19:23
 */
public class PluginAopGenerator {

    private final Map<String, byte[]> classesTypeMap = new HashMap<>();
    private final Instrumentation instrumentation;
    private final AgentBuilder agentBuilder;

    public PluginAopGenerator(Instrumentation instrumentation,
                              AgentBuilder agentBuilder) {
        this.instrumentation = instrumentation;
        this.agentBuilder = agentBuilder;
    }

    public static String bootstrapAopClass(String methodsInterceptor) {
        return methodsInterceptor + "Aop";
    }

    public AgentBuilder generate(List<AbstractPlugin> plugins) {
        for (AbstractPlugin plugin : plugins) {
            generateAop4Plugin(plugin);
        }
        return injectClassToClassLoader();
    }

    private void generateAop4Plugin(AbstractPlugin plugin) {
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
            ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(instrumentation);
            factory.make(null, null).injectRaw(classesTypeMap);
            return agentBuilder.with(new AgentBuilder.InjectionStrategy.UsingUnsafe.OfFactory(factory));
        } else {
            return agentBuilder;
        }
    }

    private void generateAopClass(String interceptorClass,
                                  MethodPointCutDescriptor methodPointCutDescriptor) {
        switch (methodPointCutDescriptor.getTargetMethodType()) {
            case INSTANCE_METHOD:
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

        DynamicType.Unloaded<?> aopClassType = null;
        aopClassType = new ByteBuddy().redefine(baseBootstrapAopClass)
                                      .name(targetAopClassName)
                                      .field(ElementMatchers.named("INTERCEPTOR_CLASS_NAME"))
                                      .value(interceptorClass)
                                      .make();

        if (methodPointCutDescriptor.isDebug()) {
            AopDebugger.INSTANCE.saveClassToFile(aopClassType);
        }

        classesTypeMap.put(targetAopClassName, aopClassType.getBytes());
    }
}
