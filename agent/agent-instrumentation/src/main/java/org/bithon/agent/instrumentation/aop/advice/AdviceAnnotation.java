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

package org.bithon.agent.instrumentation.aop.advice;

import org.bithon.agent.instrumentation.aop.interceptor.installer.InstallerRecorder;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import org.bithon.shaded.net.bytebuddy.utility.JavaConstant;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author frankchen
 */
public class AdviceAnnotation {

    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface InterceptorName {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface InterceptorIndex {
    }

    public static class InterceptorNameResolver implements Advice.OffsetMapping {
        private final String name;

        public InterceptorNameResolver(String name) {
            this.name = name;
        }

        @Nonnull
        @Override
        public Target resolve(@Nonnull TypeDescription instrumentedType,
                              @Nonnull MethodDescription instrumentedMethod,
                              @Nonnull Assigner assigner,
                              @Nonnull Advice.ArgumentHandler argumentHandler,
                              @Nonnull Sort sort) {
            InstallerRecorder.INSTANCE.addInterceptedMethod(name, instrumentedType, instrumentedMethod);

            return new Target.ForStackManipulation(new JavaConstantValue(JavaConstant.Simple.ofLoaded(name)));
        }
    }

    public static class InterceptorIndexResolver implements Advice.OffsetMapping {
        private final int index;

        public InterceptorIndexResolver(int index) {
            this.index = index;
        }

        @Nonnull
        @Override
        public Target resolve(@Nonnull TypeDescription instrumentedType,
                              @Nonnull MethodDescription instrumentedMethod,
                              @Nonnull Assigner assigner,
                              @Nonnull Advice.ArgumentHandler argumentHandler,
                              @Nonnull Sort sort) {
            return new Target.ForStackManipulation(new JavaConstantValue(JavaConstant.Simple.ofLoaded(index)));
        }
    }
}
