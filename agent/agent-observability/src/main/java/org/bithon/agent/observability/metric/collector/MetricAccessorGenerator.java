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


import org.bithon.agent.observability.metric.model.IMetricAccessor;
import org.bithon.shaded.net.bytebuddy.ByteBuddy;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.implementation.Implementation;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.bithon.shaded.net.bytebuddy.jar.asm.Label;
import org.bithon.shaded.net.bytebuddy.jar.asm.MethodVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

/**
 * Generates a subclass of {@link IMetricAccessor} that provides access to fields of a given metric set class.
 */
public class MetricAccessorGenerator {

    @FunctionalInterface
    public interface IMetricsInstantiator<T> {
        T newInstance();
    }

    /**
     * Generate an instantiator that create an instance of given type {@link T}.
     * The instance returned by the initiator is type of T1, where T1 is a generated class that have the following signature
     * <pre><code>
     * class T1 extends T implements IMetricAccessor {
     * }
     * </code></pre>
     */
    public static <T> IMetricsInstantiator<T> createInstantiator(Class<T> metricSetClass) {

        List<Field> fields = Arrays.asList(metricSetClass.getDeclaredFields());

        // Use ByteBuddy to generate a subclass implementing IndexedGetter
        DynamicType.Builder<?> builder = new ByteBuddy()
            .subclass(metricSetClass)
            .implement(IMetricAccessor.class);

        try (DynamicType.Unloaded<?> type = builder
            //
            // Create 'getMetricValue' by index method
            //
            .defineMethod("getMetricValue", long.class, Visibility.PUBLIC)
            .withParameter(int.class)
            .intercept(new Implementation.Simple(new GetMetricValueMethodGenerator(fields)))
            //
            // Create 'getMetricValue' by name method
            //
            .defineMethod("getMetricValue", long.class, Visibility.PUBLIC)
            .withParameter(String.class)
            .intercept(new Implementation.Simple(new GetMetricValueByNameMethodGenerator(fields)))
            //
            // Create 'getMetricCount' method
            //
            .defineMethod("getMetricCount", int.class, Visibility.PUBLIC)
            .intercept(new Implementation.Simple((mv, context, method) -> {
                mv.visitCode();
                mv.visitLdcInsn(fields.size());
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(1, 1);
                mv.visitEnd();

                /*
                 * Operand Stack Size (1): The maximum stack size needed is 1 because the method loads a constant int value (which takes 1 slots on the operand stack) and then returns it.
                 * Local Variables (1): The method uses 1 local variable, which is the this reference.
                 */
                return new ByteCodeAppender.Size(1, 1);
            }))
            .make()) {

            //noinspection unchecked
            Class<T> clazz = (Class<T>) type.load(metricSetClass.getClassLoader())
                                            .getLoaded();

            try {
                final Constructor<T> ctor = clazz.getDeclaredConstructor();
                return () -> {
                    try {
                        //noinspection
                        return (T) ctor.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e.getTargetException() == null ? e : e.getTargetException());
                    }
                };
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class GetMetricValueMethodGenerator implements ByteCodeAppender {
        private final List<Field> fields;

        private GetMetricValueMethodGenerator(List<Field> fields) {
            this.fields = fields;
        }

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
                                      context.getInstrumentedType().getInternalName(),
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
                    context.getInstrumentedType().getInternalName(),
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
    }

    /**
     * Generate method implementation for {@link IMetricAccessor#getMetricValue(String)}.
     * It generates codes as following pattern:
     * <pre><code>
     *     if ("field".equals(fieldName)) {
     *          return this.field;
     *     }
     *     if ("field2".equals(fieldName)) {
     *          return this.field2;
     *     }
     *     throw new IllegalArgumentException("Unknown metric name: " + fieldName);
     * </code></pre>
     */
    private static class GetMetricValueByNameMethodGenerator implements ByteCodeAppender {
        private final List<Field> fields;

        private GetMetricValueByNameMethodGenerator(List<Field> fields) {
            this.fields = fields;
        }

        @Override
        public Size apply(MethodVisitor mv, Implementation.Context context, MethodDescription method) {
            mv.visitCode();
            String internalName = context.getInstrumentedType().getInternalName();

            for (Field field : fields) {
                // if ("field".equals(fieldName))
                mv.visitVarInsn(Opcodes.ALOAD, 1); // Load the method argument 'fieldName'
                mv.visitLdcInsn(field.getName());            // Load the literal field name
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                   "java/lang/String",
                                   "equals",
                                   "(Ljava/lang/Object;)Z",
                                   false);
                Label notEqual = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, notEqual);

                // If equal, return this.field
                mv.visitVarInsn(Opcodes.ALOAD, 0); // Load 'this'
                mv.visitFieldInsn(Opcodes.GETFIELD,
                                  internalName,
                                  field.getName(),
                                  "J");          // Get field (descriptor "J" for long)
                mv.visitInsn(Opcodes.LRETURN);

                mv.visitLabel(notEqual);
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null); // Ensure stack frame is updated
            }

            // throw new IllegalArgumentException("Unknown metric name: " + fieldName)
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
            mv.visitInsn(Opcodes.DUP);
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn("Unknown metric name: ");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                               "java/lang/StringBuilder",
                               "<init>",
                               "(Ljava/lang/String;)V",
                               false);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                               "java/lang/StringBuilder",
                               "append",
                               "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                               false);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                               "java/lang/StringBuilder",
                               "toString",
                               "()Ljava/lang/String;",
                               false);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                               "java/lang/IllegalArgumentException",
                               "<init>",
                               "(Ljava/lang/String;)V",
                               false);
            mv.visitInsn(Opcodes.ATHROW);

            // Return size: maxStack = 6, maxLocals = 2 (this + fieldName)
            return new Size(6, 2);
        }
    }
}
