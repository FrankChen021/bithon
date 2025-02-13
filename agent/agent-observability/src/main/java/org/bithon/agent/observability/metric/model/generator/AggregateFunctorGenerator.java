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

package org.bithon.agent.observability.metric.model.generator;

import org.bithon.agent.observability.metric.model.annotation.First;
import org.bithon.agent.observability.metric.model.annotation.Last;
import org.bithon.agent.observability.metric.model.annotation.Max;
import org.bithon.agent.observability.metric.model.annotation.Min;
import org.bithon.agent.observability.metric.model.annotation.Sum;
import org.bithon.shaded.net.bytebuddy.ByteBuddy;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.implementation.Implementation;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.bithon.shaded.net.bytebuddy.jar.asm.Label;
import org.bithon.shaded.net.bytebuddy.jar.asm.MethodVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a class that extends a given class and implements {@link IAggregate} interface.
 * The fields of the given class must be annotated by {@link Sum}, {@link Min}, {@link Max}, {@link First}, or {@link Last}
 * and declared as PUBLIC and of type long.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/2/6 23:44
 */
public class AggregateFunctorGenerator {

    public static <T> IAggregateInstanceSupplier<T> createAggregateFunctor(Class<T> targetClass) {
        // Collect all fields
        List<FieldInfo> fields = new ArrayList<>();
        for (Field field : targetClass.getDeclaredFields()) {
            if (field.getType() != long.class) {
                continue;
            }
            if (!Modifier.isPublic(field.getModifiers())) {
                continue;
            }

            if (field.isAnnotationPresent(Sum.class)) {
                fields.add(new FieldInfo(field, AggregationType.SUM));
            } else if (field.isAnnotationPresent(Min.class)) {
                fields.add(new FieldInfo(field, AggregationType.MIN));
            } else if (field.isAnnotationPresent(Max.class)) {
                fields.add(new FieldInfo(field, AggregationType.MAX));
            } else if (field.isAnnotationPresent(First.class)) {
                fields.add(new FieldInfo(field, AggregationType.FIRST));
            } else if (field.isAnnotationPresent(Last.class)) {
                fields.add(new FieldInfo(field, AggregationType.LAST));
            }
        }
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Given class does not have any PUBLIC fields annotated with aggregation annotations");
        }

        try (DynamicType.Unloaded<?> dynamicType = new ByteBuddy()
            .subclass(Object.class)
            .implement(IAggregate.class)
            .intercept(new Implementation.Simple(new AggregateMethodByteCodeGenerator(targetClass, fields)))
            .make()) {

            //noinspection unchecked
            Class<T> clazz = (Class<T>) dynamicType.load(AggregateFunctorGenerator.class.getClassLoader())
                                                   .getLoaded();
            return () -> {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | NoSuchMethodException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e.getTargetException() == null ? e : e.getTargetException());
                }
            };
        }
    }

    private static class FieldInfo {
        final String name;
        final AggregationType aggregationType;

        FieldInfo(Field field, AggregationType aggregationType) {
            this.name = field.getName();
            this.aggregationType = aggregationType;
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
                field.aggregationType.generateCode(mv, className, field.name);
            }

            mv.visitInsn(Opcodes.RETURN);

            // The maximum stack size needed is 4:
            // - 2 slots for long values
            // - 1 slot for object reference
            // - 1 slot for comparison/arithmetic
            return new Size(4, 3);  // 3 locals: this + prev + now
        }
    }

    private enum AggregationType {
        SUM {
            @Override
            public void generateCode(MethodVisitor mv, String className, String fieldName) {
                // Load prev.field
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                // Load now.field
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                // Add them
                mv.visitInsn(Opcodes.LADD);

                // Store result in prev.field
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitInsn(Opcodes.DUP_X2);
                mv.visitInsn(Opcodes.POP);
                mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");
            }
        },
        MIN {
            @Override
            public void generateCode(MethodVisitor mv, String className, String fieldName) {
                Label useSecond = new Label();
                Label endCompare = new Label();

                // Load prev.field
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                // Load now.field
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                // Compare values
                mv.visitInsn(Opcodes.LCMP);
                mv.visitJumpInsn(Opcodes.IFGT, useSecond);  // if prev > now, use now's value

                // Use prev value (already smaller or equal)
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");
                mv.visitJumpInsn(Opcodes.GOTO, endCompare);

                // Use now value
                mv.visitLabel(useSecond);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                mv.visitLabel(endCompare);
                mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.LONG});

                // Store result in prev.field
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitInsn(Opcodes.DUP_X2);
                mv.visitInsn(Opcodes.POP);
                mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");
            }
        },
        MAX {
            @Override
            public void generateCode(MethodVisitor mv, String className, String fieldName) {
                Label useSecond = new Label();
                Label endCompare = new Label();

                // Load prev.field
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                // Load now.field
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                // Compare values
                mv.visitInsn(Opcodes.LCMP);
                mv.visitJumpInsn(Opcodes.IFLT, useSecond);  // if prev < now, use now's value

                // Use prev value (already larger or equal)
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");
                mv.visitJumpInsn(Opcodes.GOTO, endCompare);

                // Use now value
                mv.visitLabel(useSecond);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");

                mv.visitLabel(endCompare);
                mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.LONG});

                // Store result in prev.field
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitInsn(Opcodes.DUP_X2);
                mv.visitInsn(Opcodes.POP);
                mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");
            }
        },
        FIRST {
            @Override
            public void generateCode(MethodVisitor mv, String className, String fieldName) {
                // DO NOTHING
            }
        },
        LAST {
            @Override
            public void generateCode(MethodVisitor mv, String className, String fieldName) {
                // For @Last, we always take the value from now
                mv.visitVarInsn(Opcodes.ALOAD, 1);  // load prev
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitVarInsn(Opcodes.ALOAD, 2);  // load now
                mv.visitTypeInsn(Opcodes.CHECKCAST, className);
                mv.visitFieldInsn(Opcodes.GETFIELD, className, fieldName, "J");  // get now.field
                mv.visitFieldInsn(Opcodes.PUTFIELD, className, fieldName, "J");  // set prev.field

            }
        };

        /**
         * <pre>
         * Generate byte code for the {@link IAggregate#aggregate(Object, Object)} for a given aggregate type.
         * For {@link #SUM}, it generates code as: <code>prev.field += now.field</code>
         * For {@link #MIN}, it generates code as: <code>prev.field = prev.field < now.field ? prev.field : now.field</code>
         * For {@link #MAX}, it generates code as: <code>prev.field = prev.field > now.field ? prev.field : now.field</code>
         * For {@link #FIRST}, it does nothing.
         * For {@link #LAST}, it generates code as: <code>prev.field = now.field</code>
         * </pre>
         */
        public abstract void generateCode(MethodVisitor mv, String className, String fieldName);
    }
}
