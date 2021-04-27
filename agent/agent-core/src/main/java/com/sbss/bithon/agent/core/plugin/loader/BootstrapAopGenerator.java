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

package com.sbss.bithon.agent.core.plugin.loader;


import com.sbss.bithon.agent.bootstrap.aop.BootstrapConstructorAop;
import com.sbss.bithon.agent.bootstrap.aop.BootstrapMethodAop;
import com.sbss.bithon.agent.bootstrap.expt.AgentException;
import com.sbss.bithon.agent.bootstrap.loader.AgentDependencyManager;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.debug.TransformationDebugger;
import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.descriptor.MethodPointCutDescriptor;
import shaded.net.bytebuddy.ByteBuddy;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.net.bytebuddy.matcher.ElementMatchers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class BootstrapAopGenerator {

    private final Map<String, byte[]> classesTypeMap = new HashMap<>();
    private final Instrumentation instrumentation;
    private final AgentBuilder agentBuilder;

    public BootstrapAopGenerator(Instrumentation instrumentation,
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

    /**
     * ALWAYS inject the classes below into bootstrap class loader even if there's no instrumentation for JDK classes
     * This would help prevent potential bugs from plugins which cause these classes be loaded by system class loader
     */
    private AgentBuilder injectClassToClassLoader() {
        // inject byte buddy annotation classes into bootstrap class loader
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.RuntimeType");
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.This");
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.AllArguments");
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.AllArguments$Assignment");
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.SuperCall");
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.Origin");
        this.inject("shaded.net.bytebuddy.implementation.bind.annotation.Morph");

        ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(instrumentation);
        factory.make(null, null).injectRaw(classesTypeMap);
        return agentBuilder.with(new AgentBuilder.InjectionStrategy.UsingUnsafe.OfFactory(factory));
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
            new TransformationDebugger().saveClassToFile(aopClassType);
        }

        classesTypeMap.put(targetAopClassName, aopClassType.getBytes());
    }

    /**
     * load class bytes from resource to avoid loading of target class by system class loader
     */
    private void inject(String className) {
        String classResourceName = className.replaceAll("\\.", "/") + ".class";
        try {
            try (InputStream resourceAsStream = AgentDependencyManager.getClassLoader()
                                                                      .getResourceAsStream(classResourceName)) {
                if (resourceAsStream == null) {
                    throw new AgentException("Class [%s] for bootstrap injection not found", className);
                }

                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[2048];
                    int len;

                    while ((len = resourceAsStream.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }

                    this.classesTypeMap.put(className, os.toByteArray());
                }
            }
        } catch (IOException e) {
            throw new AgentException("Failed to load class [%s]: %s", className, e.getMessage());
        }
    }
}
