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

package org.bithon.agent.plugin.guice.installer;

import org.bithon.agent.core.aop.AopDebugger;
import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.agent.core.utils.bytecode.ByteCodeUtils;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.field.FieldDescription;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:05
 */
public class BeanMethodAopInstaller {
    private static final Logger log = LoggerFactory.getLogger(BeanMethodAopInstaller.class);

    private static final Set<String> INSTRUMENTED = new ConcurrentSkipListSet<>();

    public static void initialize() {
        //
        // inject interceptor classes into bootstrap class loader to ensure this interceptor classes could be found by Adviced code which would be loaded by application class loader
        // because for any class loader, it would back to bootstrap class loader to find class first
        //
        ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance()).make(null, null).injectRaw(new HashMap<String, byte[]>() {
            {
                put(BeanMethodInterceptorIntf.class.getName(),
                    ByteCodeUtils.getClassByteCode(BeanMethodInterceptorIntf.class.getName(), BeanMethodInterceptorIntf.class.getClassLoader()));
                put(BeanMethodInterceptorTemplate.class.getName(),
                    ByteCodeUtils.getClassByteCode(BeanMethodInterceptorTemplate.class.getName(), BeanMethodInterceptorTemplate.class.getClassLoader()));
            }
        });
    }

    public static void install(Class<?> clazz) {
        if (clazz.isSynthetic()) {
            return;
        }

        if (!INSTRUMENTED.add(clazz.getName())) {
            return;
        }

        log.info("Setup AOP for Guice Bean class [{}]", clazz.getName());

        new AgentBuilder.Default().ignore(ElementMatchers.none())
                                  .disableClassFormatChanges()
                                  .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                  .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                  .type(ElementMatchers.is(clazz))
                                  .transform((builder, typeDescription, classLoader, javaModule) -> {

                                      //
                                      // infer property methods
                                      //
                                      Set<String> propertyMethods = new HashSet<>();
                                      for (FieldDescription field : typeDescription.getDeclaredFields()) {
                                          String name = field.getName();
                                          char[] chr = name.toCharArray();
                                          chr[0] = Character.toUpperCase(chr[0]);
                                          name = new String(chr);
                                          propertyMethods.add("get" + name);
                                          propertyMethods.add("set" + name);
                                          propertyMethods.add("is" + name);
                                      }

                                      //
                                      // inject on corresponding methods
                                      //
                                      return builder.visit(Advice.to(BeanMethodAop.class)
                                                                 .on(new BeanMethodModifierMatcher().and((method -> !propertyMethods.contains(method.getName())))));

                                  })
                                  .with(AopDebugger.INSTANCE)
                                  .installOn(InstrumentationHelper.getInstance());
    }

    static class BeanMethodModifierMatcher extends ElementMatcher.Junction.AbstractBase<MethodDescription> {
        @Override
        public boolean matches(MethodDescription target) {
            return target.isPublic()
                   && !target.isConstructor()
                   && !target.isStatic()
                   && !target.isAbstract()
                   && !target.isNative()
                   && !target.getName().startsWith("is")
                   && !target.getName().startsWith("set");
        }
    }
}
