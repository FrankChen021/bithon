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

package org.bithon.agent.instrumentation.utils;


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
import org.bithon.shaded.net.bytebuddy.jar.asm.Type;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// Interface for field access


// Original class with Long fields


public class MetricAccessorGenerator {

    public interface IndexedGetter {
        long get(int i);
    }

    public static class SampleData {
        public long field0 = 1;
        public long field1 = 2;
        public long field2 = 3;
        // Original class without the dynamic method
    }

    public static void main(String[] args) throws Exception {
        // Retrieve all Long fields from MyClass
        List<Field> longFields = Arrays.stream(SampleData.class.getDeclaredFields())
                                       .filter(field -> field.getType().equals(Long.class))
                                       .collect(Collectors.toList());

        // Use ByteBuddy to generate a subclass implementing IndexedGetter
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(SampleData.class)
            .implement(IndexedGetter.class);

        // Define the `get(int i)` method with manually written logic
        DynamicType.Unloaded type = builder
            .defineMethod("get", long.class, Visibility.PUBLIC)
            .withParameters(int.class)
            .intercept(new FieldValueByIndexImplementation(SampleData.class))
            .make();

        type.saveIn(new File("/Users/frankchen/source/test.txt"));
        Class dynamicType = type.load(SampleData.class.getClassLoader())
                                .getLoaded();

        // Create an instance of the dynamically generated class
        SampleData instance = (SampleData) dynamicType.getDeclaredConstructor().newInstance();
        IndexedGetter getter = (IndexedGetter) instance;

        // Test method calls
        System.out.println(getter.get(0)); // Expected: 10
        System.out.println(getter.get(1)); // Expected: 20
        System.out.println(getter.get(2)); // Expected: 30
    }

    public static class FieldValueByIndexImplementation implements Implementation {

        private final List<Field> fields = new ArrayList<>();
        private String targetClass;

        public FieldValueByIndexImplementation(Class<?> targetClass) {
            for (Field field : targetClass.getDeclaredFields()) {
                fields.add(field);
            }
            this.targetClass = targetClass.getName().replace('.', '/');
        }

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return new ByteCodeAppender() {
                @Override
                public Size apply(MethodVisitor mv, Implementation.Context context, MethodDescription method) {
                    mv.visitCode();

                    // Table switch implementation
                    Label defaultLabel = new Label();
                    Label[] fieldLabels = new Label[fields.size()];
                    for (int i = 0; i < fields.size(); i++) {
                        fieldLabels[i] = new Label();
                    }

                    // Load index and create tableswitch
                    mv.visitVarInsn(Opcodes.ILOAD, 1);
                    mv.visitTableSwitchInsn(0, fields.size() - 1, defaultLabel, fieldLabels);

                    // Generate field access code for each field
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        mv.visitLabel(fieldLabels[i]);
                        // FULL_FRAME for first label, SAME for others
                        if (i == 0) {
                            // F_FULL frame with 2 locals (this + int parameter)
                            mv.visitFrame(Opcodes.F_FULL,
                                          2,
                                          new Object[]{
                                              targetClass,
                                              Opcodes.INTEGER
                                          }, 0, new Object[]{});
                        } else {
                            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }

                        // Load 'this' reference
                        mv.visitVarInsn(Opcodes.ALOAD, 0);

                        // Get field value (using GETFIELD for long)
                        mv.visitFieldInsn(
                            Opcodes.GETFIELD,
                            targetClass,
                            field.getName(),
                            "J"  // descriptor for long
                        );

                        // Return long value directly
                        mv.visitInsn(Opcodes.LRETURN);
                    }

                    // Default case - throw IllegalArgumentException
                    mv.visitLabel(defaultLabel);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("Invalid field index");
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                       "java/lang/IllegalArgumentException",
                                       "<init>",
                                       "(Ljava/lang/String;)V",
                                       false);
                    mv.visitInsn(Opcodes.ATHROW);

                    // Maximum stack size needed is Max(2, 3), where
                    // - 2 slots for long values (they take 2 slots)
                    // - 3 slots for exception creation (NEW, DUP, LDC)
                    // Local variables are 2 (this + index parameter)
                    return new Size(3, 2);
                }
            };
        }

        public ByteCodeAppender appender_v3(Target implementationTarget) {
            return new ByteCodeAppender() {
                @Override
                public Size apply(MethodVisitor mv, Implementation.Context context, MethodDescription method) {
                    mv.visitCode();

                    // Table switch implementation
                    Label defaultLabel = new Label();
                    Label[] fieldLabels = new Label[fields.size()];
                    for (int i = 0; i < fields.size(); i++) {
                        fieldLabels[i] = new Label();
                    }

                    // Load index and create tableswitch
                    mv.visitVarInsn(Opcodes.ILOAD, 1);
                    mv.visitTableSwitchInsn(0, fields.size() - 1, defaultLabel, fieldLabels);

                    // Generate field access code for each field
                    for (int i = 0; i < fields.size(); i++) {
                        Field field = fields.get(i);
                        mv.visitLabel(fieldLabels[i]);
                        // FULL_FRAME for first label, SAME for others
                        if (i == 0) {
                            // F_FULL frame with 2 locals (this + int parameter)
                            mv.visitFrame(Opcodes.F_FULL, 2,
                                          new Object[]{
                                              targetClass,
                                              Opcodes.INTEGER
                                          }, 0, new Object[]{});
                        } else {
                            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }

                        // Load 'this' reference
                        mv.visitVarInsn(Opcodes.ALOAD, 0);

                        // Get field value
                        mv.visitFieldInsn(
                            Opcodes.GETFIELD,
                            targetClass,
                            field.getName(),
                            Type.getDescriptor(field.getType())
                        );

                        // Box primitive types if necessary
                        if (field.getType().isPrimitive()) {
                            boxPrimitiveType(mv, field.getType());
                        }

                        mv.visitInsn(Opcodes.ARETURN);
                    }

                    // Default case - throw IllegalArgumentException
                    mv.visitLabel(defaultLabel);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("Invalid field index");
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                       "java/lang/IllegalArgumentException",
                                       "<init>",
                                       "(Ljava/lang/String;)V",
                                       false);
                    mv.visitInsn(Opcodes.ATHROW);

                    // Maximum stack size needed is 3 (for exception creation)
                    // Local variables are 2 (this + index parameter)
                    return new Size(3, 2);
                }
            };
        }

        private static void boxPrimitiveType(MethodVisitor mv, Class<?> type) {
            if (type == int.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (type == long.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (type == double.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            } else if (type == float.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (type == boolean.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (type == byte.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (type == short.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (type == char.class) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            }
        }

        public ByteCodeAppender appender_ok(Target implementationTarget) {
            return new ByteCodeAppender() {
                @Override
                public Size apply(MethodVisitor mv, Implementation.Context context, MethodDescription method) {
                    // Start of method
                    mv.visitCode();

                    // Create labels for each case
                    Label label0 = new Label();
                    Label label1 = new Label();
                    Label label2 = new Label();
                    Label defaultLabel = new Label();
                    Label endLabel = new Label();

                    // Compare index with 0
                    mv.visitVarInsn(Opcodes.ILOAD, 1);  // Load index parameter
                    mv.visitJumpInsn(Opcodes.IFEQ, label0);

                    // Add stack map frame before next comparison
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    // Compare index with 1
                    mv.visitVarInsn(Opcodes.ILOAD, 1);
                    mv.visitInsn(Opcodes.ICONST_1);
                    mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label1);

                    // Add stack map frame
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    // Compare index with 2
                    mv.visitVarInsn(Opcodes.ILOAD, 1);
                    mv.visitInsn(Opcodes.ICONST_2);
                    mv.visitJumpInsn(Opcodes.IF_ICMPEQ, label2);

                    // Add stack map frame
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    // Default case - throw IllegalArgumentException
                    mv.visitJumpInsn(Opcodes.GOTO, defaultLabel);

                    // Case index == 0
                    mv.visitLabel(label0);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, "SampleData", "field0", "Ljava/lang/String;");
                    mv.visitJumpInsn(Opcodes.GOTO, endLabel);

                    // Case index == 1
                    mv.visitLabel(label1);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, "SampleData", "field1", "Ljava/lang/String;");
                    mv.visitJumpInsn(Opcodes.GOTO, endLabel);

                    // Case index == 2
                    mv.visitLabel(label2);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, "SampleData", "field2", "Ljava/lang/String;");
                    mv.visitJumpInsn(Opcodes.GOTO, endLabel);

                    // Default case - throw IllegalArgumentException
                    mv.visitLabel(defaultLabel);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("Invalid index");
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
                    mv.visitInsn(Opcodes.ATHROW);

                    // End of method
                    mv.visitLabel(endLabel);
                    mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    mv.visitInsn(Opcodes.ARETURN);

                    // Method stack size and locals
                    return new Size(4, 2);
                }
            };
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }
    }
}
