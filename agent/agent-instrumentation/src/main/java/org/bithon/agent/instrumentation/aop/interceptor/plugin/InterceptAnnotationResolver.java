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

package org.bithon.agent.instrumentation.aop.interceptor.plugin;

import org.bithon.agent.instrumentation.aop.interceptor.Intercept;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorType;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodType;
import org.bithon.agent.instrumentation.aop.interceptor.expression.ExpressionMatcher;
import org.bithon.agent.instrumentation.aop.interceptor.expression.parser.ExpressionParser;
import org.bithon.shaded.net.bytebuddy.jar.asm.AnnotationVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassReader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.InterceptorType.AFTER;
import static org.bithon.agent.instrumentation.aop.interceptor.InterceptorType.AROUND;
import static org.bithon.agent.instrumentation.aop.interceptor.InterceptorType.BEFORE;
import static org.bithon.agent.instrumentation.aop.interceptor.InterceptorType.REPLACEMENT;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/28 21:57
 */
public class InterceptAnnotationResolver {

    public InterceptorDescriptor resolve(InputStream is, ClassLoader classLoader) throws IOException {
        AnnotationMetaExtractor visitor = new AnnotationMetaExtractor();

        ClassReader cr = new ClassReader(is);
        cr.accept(visitor, ClassReader.EXPAND_FRAMES);

        String superName = visitor.getSuperName();
        InterceptorType type = null;
        if (AROUND.type().equals(superName)) {
            type = AROUND;
        } else if (BEFORE.type().equals(superName)) {
            type = BEFORE;
        } else if (AFTER.type().equals(superName)) {
            type = AFTER;
        } else if (REPLACEMENT.type().equals(superName)) {
            type = InterceptorType.REPLACEMENT;
        } else {
            // For any recognized super name, check if the super implements the required interface
            //type = new InterceptorTypeResolver(classLoader).resolve(superName);
        }

        InterceptorDescriptor descriptor = null;
        for (String expression : visitor.expressions()) {
            ExpressionMatcher matcher = ExpressionParser.parse(expression);
            MethodPointCutDescriptor methodPointCut = new MethodPointCutDescriptor(false,
                                                                                   matcher.getMethodMatcher(),
                                                                                   matcher.isCtor() ? MethodType.CONSTRUCTOR : MethodType.NON_CONSTRUCTOR,
                                                                                   superName);
            methodPointCut.setInterceptorType(type);
        }

        return descriptor;
    }

    static class AnnotationExtractor extends AnnotationVisitor {
        private final List<String> expressions = new ArrayList<>();

        protected AnnotationExtractor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            if ("expressions".equals(name)) {
                return this;
            }
            return null;
        }

        @Override
        public void visit(String name, Object value) {
            if (name == null) {
                // Visiting value in array
                expressions.add((String) value);
            }
        }
    }

    static class AnnotationMetaExtractor extends ClassVisitor {
        private final String ANNOTATION = "L" + Intercept.class.getName().replace('.', '/') + ";";

        private String superName;
        private final AnnotationExtractor annotationExtractor = new AnnotationExtractor();

        protected AnnotationMetaExtractor() {
            super(Opcodes.ASM9);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (ANNOTATION.equals(descriptor)) {
                return annotationExtractor;
            }
            return null;
        }

        @Override
        public void visit(int version,
                          int access,
                          String name,
                          String signature,
                          String superName,
                          String[] interfaces) {
            this.superName = superName;
        }

        public String getSuperName() {
            return superName;
        }

        public List<String> expressions() {
            return annotationExtractor.expressions;
        }
    }
}
