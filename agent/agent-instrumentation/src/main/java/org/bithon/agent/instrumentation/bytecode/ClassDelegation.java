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

package org.bithon.agent.instrumentation.bytecode;

import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.shaded.net.bytebuddy.ByteBuddy;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import org.bithon.shaded.net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import org.bithon.shaded.net.bytebuddy.implementation.FieldAccessor;
import org.bithon.shaded.net.bytebuddy.implementation.MethodCall;
import org.bithon.shaded.net.bytebuddy.implementation.MethodDelegation;
import org.bithon.shaded.net.bytebuddy.implementation.bind.MethodDelegationBinder;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate a subclass that satisfies the base class and delegate all its public method to a delegated object.
 * <p>
 * For example,
 * <pre>
 *     class A {
 *         public void getA() {
 *
 *         }
 *     }
 * </pre>
 *
 * The generated class:
 * <pre>
 *     class GeneratedA extends A implements IDelegation {
 *         private A delegation;
 *
 *         public GeneratedA(Object delegation) {
 *             super();
 *             this.delegation = delegation;
 *         }
 *
 *         public void getA() {
 *            delegation.getA();
 *         }
 *
 *         public void setDelegate(Object val) {
 *             this.delegation = val;
 *         }
 *
 *         public Class getDelegationClass() {
 *             return delegation.getClass();
 *         }
 *     }
 * </pre>
 * <p>
 * Constraints:
 * 1. base class must have a default ctor
 *
 * @author frank.chen021@outlook.com
 * @date 2023/1/7 15:13
 */
public class ClassDelegation {

    public static Class<?> create(Class<?> baseClass) {
        Constructor<?> defaultCtor;
        try {
            defaultCtor = baseClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new AgentException("The lass [%s] does not define a default constructor.", baseClass.getName());
        }

        ElementMatcher<? super MethodDescription> delegationMethods = ElementMatchers.isMethod()
                                                                                     .and(ElementMatchers.<MethodDescription>isPublic())
                                                                                     .and(ElementMatchers.not(ElementMatchers.isNative()));

        DynamicType.Unloaded<?> type = new ByteBuddy().subclass(baseClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                                                      .implement(IDelegation.class)
                                                      // Create delegation field
                                                      .defineField("delegation", baseClass, Modifier.PRIVATE)
                                                      // Override all public and non-native methods to call methods on the delegation object
                                                      .method(delegationMethods)
                                                      .intercept(MethodDelegation.withDefaultConfiguration()
                                                                                 .withBindingResolver(new MethodSignatureMatchResolver())
                                                                                 .filter(delegationMethods)
                                                                                 .toField("delegation"))
                                                      // Define IDelegation.getDelegationClass
                                                      .defineMethod("getDelegationClass", Class.class, Visibility.PUBLIC)
                                                      .intercept(MethodCall.invoke(ElementMatchers.named("getClass")).onField("delegation"))
                                                      // Define IDelegation.setDelegation
                                                      .defineMethod("setDelegation", void.class, Visibility.PUBLIC).withParameters(Object.class)
                                                      .intercept(FieldAccessor.ofField("delegation")
                                                                              .withAssigner(Assigner.GENERICS_AWARE, Assigner.Typing.DYNAMIC)
                                                                              .setsArgumentAt(0))
                                                      // Create constructor
                                                      .defineConstructor(Visibility.PUBLIC)
                                                      .withParameters(baseClass)
                                                      .intercept(// Call super ctor first
                                                                 MethodCall.invoke(defaultCtor)
                                                                           // and then Assign delegation
                                                                           .andThen(FieldAccessor.ofField("delegation").setsArgumentAt(0)))
                                                      .make();

        //
        // Inject this dynamic class into class loader
        //
        Map<String, byte[]> typeMap = new HashMap<>();
        typeMap.put(type.getTypeDescription().getTypeName(), type.getBytes());

        ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance());
        factory.make(baseClass.getClassLoader(), null).injectRaw(typeMap);
        try {
            return baseClass.getClassLoader()
                            .loadClass(type.getTypeDescription().getTypeName());
        } catch (ClassNotFoundException e) {
            throw new AgentException(e);
        } finally {
            try {
                type.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Pick a target method that has the same signature as the source
     */
    private static class MethodSignatureMatchResolver implements MethodDelegationBinder.BindingResolver {
        public MethodDelegationBinder.MethodBinding resolve(MethodDelegationBinder.AmbiguityResolver ambiguityResolver,
                                                            MethodDescription source,
                                                            List<MethodDelegationBinder.MethodBinding> targets) {
            if (targets.size() == 1) {
                return targets.get(0);
            }

            for (MethodDelegationBinder.MethodBinding targetBinding : targets) {
                MethodDescription target = targetBinding.getTarget();

                if (!target.getName().equals(source.getName())) {
                    continue;
                }
                if (target.getParameters().size() != source.getParameters().size()) {
                    continue;
                }
                int i = 0;
                for (; i < target.getParameters().size(); i++) {
                    if (!target.getParameters().get(i).equals(source.getParameters().get(i))) {
                        break;
                    }
                }
                if (i == target.getParameters().size()) {
                    return targetBinding;
                }
            }

            throw new IllegalStateException(source + " allowed for more than one binding: " + targets);
        }
    }
}
