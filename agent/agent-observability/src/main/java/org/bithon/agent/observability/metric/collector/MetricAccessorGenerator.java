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
import java.util.Arrays;
import java.util.List;

// Interface for field access


// Original class with Long fields


public class MetricAccessorGenerator {

    public interface IndexedGetter {
        long get(int i);
    }

    public static <T> Class<T> generate(Class<T> metricSetClass) {
        // Use ByteBuddy to generate a subclass implementing IndexedGetter
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(metricSetClass)
            .implement(IndexedGetter.class);

        // Define the `get(int i)` method with manually written logic
        DynamicType.Unloaded<?> type = builder
            .defineMethod("get", long.class, Visibility.PUBLIC)
            .withParameters(int.class)
            .intercept(new FieldValueByIndexImplementation(metricSetClass))
            .make();

        return (Class<T>) type.load(metricSetClass.getClassLoader())
                              .getLoaded();
    }

    private static class FieldValueByIndexImplementation implements Implementation {

        private final List<Field> fields = new ArrayList<>();
        private final String targetClass;

        public FieldValueByIndexImplementation(Class<?> targetClass) {
            fields.addAll(Arrays.asList(targetClass.getDeclaredFields()));
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

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }
    }
}
