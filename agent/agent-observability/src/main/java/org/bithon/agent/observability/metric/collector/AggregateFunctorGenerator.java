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

package org.bithon.agent.observability.metric.collector;

import org.bithon.agent.observability.metric.model.annotation.First;
import org.bithon.agent.observability.metric.model.annotation.Last;
import org.bithon.agent.observability.metric.model.annotation.Max;
import org.bithon.agent.observability.metric.model.annotation.Min;
import org.bithon.agent.observability.metric.model.annotation.Sum;
import org.bithon.shaded.net.bytebuddy.ByteBuddy;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.dynamic.scaffold.InstrumentedType;
import org.bithon.shaded.net.bytebuddy.implementation.Implementation;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.bithon.shaded.net.bytebuddy.jar.asm.Label;
import org.bithon.shaded.net.bytebuddy.jar.asm.MethodVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/6 23:44
 */
public class AggregateFunctorGenerator {

    private enum AggregationType {
        SUM,
        MIN,
        MAX,
        FIRST,
        LAST
    }

    private static class FieldInfo {
        final String name;
        final AggregationType aggregationType;

        FieldInfo(Field field) {
            this.name = field.getName();

            if (field.isAnnotationPresent(Sum.class)) {
                this.aggregationType = AggregationType.SUM;
            } else if (field.isAnnotationPresent(Min.class)) {
                this.aggregationType = AggregationType.MIN;
            } else if (field.isAnnotationPresent(Max.class)) {
                this.aggregationType = AggregationType.MAX;
            } else if (field.isAnnotationPresent(First.class)) {
                this.aggregationType = AggregationType.FIRST;
            } else if (field.isAnnotationPresent(Last.class)) {
                this.aggregationType = AggregationType.LAST;
            } else {
                throw new IllegalArgumentException("Field " + field.getName() +
                                                   " must have either @Sum, @Min, @Max, @First, or @Last annotation");
            }
        }
    }

    public static Class<?> createAggregateFunctor(Class<?> targetClass) {
        // Collect all fields
        List<FieldInfo> fields = new ArrayList<>();
        for (Field field : targetClass.getDeclaredFields()) {
            if (field.getType() != long.class) {
                throw new IllegalArgumentException("Field " + field.getName() + " is not of type long");
            }
            fields.add(new FieldInfo(field));
        }

        try (DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
            .subclass(targetClass)
            .defineMethod("aggregate", void.class, Visibility.PUBLIC)
            .withParameters(targetClass, targetClass)
            .intercept(new Implementation() {

                @Override
                public ByteCodeAppender appender(Target implementationTarget) {
                    return new AggregateMethodByteCodeGenerator(targetClass, fields);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType;
                }
            })
            .make()) {

            // Load and return the generated class
            return dynamicType.load(AggregateFunctorGenerator.class.getClassLoader())
                              .getLoaded();
        }
    }

    private static class AggregateMethodByteCodeGenerator implements ByteCodeAppender {
        private final String className;
        private final List<FieldInfo> fields;

        public AggregateMethodByteCodeGenerator(Class<?> targetClass, List<FieldInfo> fields) {
            this.className = targetClass.getName().replace('.', '/');
            this.fields = fields;
        }

        @Override
        public Size apply(MethodVisitor mv, Implementation.Context context, MethodDescription method) {
            mv.visitCode();

            // Process each field
            for (FieldInfo field : fields) {
                switch (field.aggregationType) {
                    case SUM:
                        generateSumLogic(mv, className, field.name);
                        break;
                    case MIN:
                        generateMinLogic(mv, className, field.name);
                        break;
                    case MAX:
                        generateMaxLogic(mv, className, field.name);
                        break;
                    case FIRST:
                        // For FIRST, do nothing (keep prev value)
                        break;
                    case LAST:
                        generateLastLogic(mv, className, field.name);
                        break;
                }
            }

            mv.visitInsn(Opcodes.RETURN);

            // Maximum stack size needed is 4:
            // - 2 slots for long values
            // - 1 slot for object reference
            // - 1 slot for comparison/arithmetic
            return new Size(4, 3);  // 3 locals: this + prev + now
        }

        private void generateSumLogic(MethodVisitor mv, String className, String fieldName) {
            // Load prev.field
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            // Load now.field
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            // Add them
            mv.visitInsn(Opcodes.LADD);

            // Store result in prev.field
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");
        }

        private void generateMinLogic(MethodVisitor mv, String className, String fieldName) {
            Label useSecond = new Label();
            Label endCompare = new Label();

            // Load prev.field
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            // Load now.field
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            // Compare values
            mv.visitInsn(Opcodes.LCMP);
            mv.visitJumpInsn(Opcodes.IFGT, useSecond);  // if prev > now, use now's value

            // Use prev value (already smaller or equal)
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");
            mv.visitJumpInsn(Opcodes.GOTO, endCompare);

            // Use now value
            mv.visitLabel(useSecond);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            mv.visitLabel(endCompare);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.LONG});

            // Store result in prev.field
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");
        }

        private void generateMaxLogic(MethodVisitor mv, String className, String fieldName) {
            Label useSecond = new Label();
            Label endCompare = new Label();

            // Load prev.field
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            // Load now.field
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            // Compare values
            mv.visitInsn(Opcodes.LCMP);
            mv.visitJumpInsn(Opcodes.IFLT, useSecond);  // if prev < now, use now's value

            // Use prev value (already larger or equal)
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");
            mv.visitJumpInsn(Opcodes.GOTO, endCompare);

            // Use now value
            mv.visitLabel(useSecond);
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

            mv.visitLabel(endCompare);
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.LONG});

            // Store result in prev.field
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");
        }

        private void generateLastLogic(MethodVisitor mv, String className, String fieldName) {
            // For @Last, we always take the value from now
            mv.visitVarInsn(Opcodes.ALOAD, 1);  // load prev
            mv.visitVarInsn(Opcodes.ALOAD, 2);  // load now
            mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");  // get now.field
            mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");  // set prev.field
        }
    }
}