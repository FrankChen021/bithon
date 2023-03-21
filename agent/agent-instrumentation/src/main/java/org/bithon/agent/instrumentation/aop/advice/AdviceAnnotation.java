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

import org.bithon.agent.instrumentation.aop.interceptor.IInterceptor;
import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.annotation.AnnotationList;
import org.bithon.shaded.net.bytebuddy.description.field.FieldDescription;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.constant.MethodConstant;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author frankchen
 */
public class AdviceAnnotation {

    /**
     * Custom annotation used on Advice classes to reference the {@link org.bithon.agent.instrumentation.aop.interceptor.IInterceptor} object
     *
     * @author frank.chen021@outlook.com
     * @date 18/2/22 8:01 PM
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface Interceptor {
    }

    /**
     * DO NOT USE on Advice which is used for re-transformation.
     * See <a href="https://github.com/raphw/byte-buddy/issues/1210">this issue</a> on github for more details
     *
     * Work with {@link TargetMethodResolver}
     *
     * @author frank.chen021@outlook.com
     * @date 22/2/22 8:21 PM
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.PARAMETER)
    public @interface TargetMethod {
    }

    /**
     * Resolve the {@link Interceptor} annotation
     *
     * @author frank.chen021@outlook.com
     * @date 19/2/22 3:47 PM
     */
    public static class InterceptorResolver extends FieldDescription.InDefinedShape.AbstractBase {
        private static final TypeDescription.Generic INTERCEPTOR_TYPE = new TypeDescription.ForLoadedType(IInterceptor.class).asGenericType();

        private final TypeDescription declaringType;
        private final String fieldName;

        public InterceptorResolver(@Nonnull TypeDescription declaringType,
                                   String fieldName) {
            this.declaringType = declaringType;
            this.fieldName = fieldName;
        }

        @Nonnull
        @Override
        public TypeDescription getDeclaringType() {
            return declaringType;
        }

        @Nonnull
        @Override
        public TypeDescription.Generic getType() {
            return INTERCEPTOR_TYPE;
        }

        @Override
        public int getModifiers() {
            return Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;
        }

        @Nonnull
        @Override
        public String getName() {
            return fieldName;
        }

        @Nonnull
        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }
    }

    /**
     * Resolve {@link TargetMethod} annotation
     *
     * See <a href="https://github.com/raphw/byte-buddy/issues/1210">this issue</a> on GitHub for more details
     *
     * @author frank.chen021@outlook.com
     * @date 22/2/22 8:30 PM
     */
    public static class TargetMethodResolver implements Advice.OffsetMapping {
        @Nonnull
        @Override
        public Target resolve(@Nonnull TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              @Nonnull Assigner assigner,
                              @Nonnull Advice.ArgumentHandler argumentHandler,
                              @Nonnull Sort sort) {
            return new Target.ForStackManipulation(MethodConstant.of(instrumentedMethod.asDefined()).cached());
        }
    }
}
