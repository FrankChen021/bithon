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

package org.bithon.agent.instrumentation.aop.interceptor.installer;

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.asm.AsmVisitorWrapper;
import org.bithon.shaded.net.bytebuddy.description.field.FieldDescription;
import org.bithon.shaded.net.bytebuddy.description.field.FieldList;
import org.bithon.shaded.net.bytebuddy.description.method.MethodList;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.implementation.Implementation;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassWriter;
import org.bithon.shaded.net.bytebuddy.jar.asm.FieldVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.MethodVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;
import org.bithon.shaded.net.bytebuddy.jar.asm.Type;
import org.bithon.shaded.net.bytebuddy.pool.TypePool;

import java.util.Arrays;

/**
 * Injects IBithonObject interface and the required field and accessor methods using ASM.
 * This is more performant than using ByteBuddy's high-level API for adding interfaces and fields.
 * 
 * @author frank.chen021@outlook.com
 * @date 2024/1/2 10:00 上午
 */
public final class BithonObjectInjector implements AsmVisitorWrapper {

    private static final ILogger log = LoggerFactory.getLogger(BithonObjectInjector.class);

    static final String IBITHON_OBJECT_INTERNAL_NAME = Type.getInternalName(IBithonObject.class);
    static final String OBJECT_DESCRIPTOR = Type.getDescriptor(Object.class);
    
    static final String GETTER_METHOD = "getInjectedObject";
    static final String GETTER_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.getType(Object.class));
    
    static final String SETTER_METHOD = "setInjectedObject";
    static final String SETTER_METHOD_DESCRIPTOR = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class));

    @Override
    public int mergeWriter(final int flags) {
        return flags | ClassWriter.COMPUTE_MAXS;
    }

    @Override
    public int mergeReader(final int flags) {
        return flags;
    }

    @Override
    public ClassVisitor wrap(
            final TypeDescription instrumentedType,
            final ClassVisitor classVisitor,
            final Implementation.Context implementationContext,
            final TypePool typePool,
            final FieldList<FieldDescription.InDefinedShape> fields,
            final MethodList<?> methods,
            final int writerFlags,
            final int readerFlags) {
        
        // Only inject if the class doesn't already implement IBithonObject
        if (instrumentedType.isAssignableTo(IBithonObject.class)) {
            return classVisitor;
        }

        return new ClassVisitor(Opcodes.ASM8, classVisitor) {
            
            private boolean foundField = false;
            private boolean foundGetter = false;
            private boolean foundSetter = false;
            private String instrumentedName;

            @Override
            public void visit(
                    final int version,
                    final int access,
                    final String name,
                    String signature,
                    final String superName,
                    String[] interfaces) {
                
                instrumentedName = name;
                
                if (interfaces == null) {
                    interfaces = new String[]{};
                }

                // Check if IBithonObject interface is already implemented
                if (!Arrays.asList(interfaces).contains(IBITHON_OBJECT_INTERNAL_NAME)) {
                    if (signature != null) {
                        signature += 'L' + IBITHON_OBJECT_INTERNAL_NAME + ';';
                    }

                    interfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
                    interfaces[interfaces.length - 1] = IBITHON_OBJECT_INTERNAL_NAME;
                }

                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(
                    final int access,
                    final String name,
                    final String descriptor,
                    final String signature,
                    final Object value) {
                if (IBithonObject.INJECTED_FIELD_NAME.equals(name)) {
                    foundField = true;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(
                    final int access,
                    final String name,
                    final String descriptor,
                    final String signature,
                    final String[] exceptions) {
                if (GETTER_METHOD.equals(name) && GETTER_METHOD_DESCRIPTOR.equals(descriptor)) {
                    foundGetter = true;
                } else if (SETTER_METHOD.equals(name) && SETTER_METHOD_DESCRIPTOR.equals(descriptor)) {
                    foundSetter = true;
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }

            @Override
            public void visitEnd() {
                if (!foundField) {
                    addInjectedField();
                }
                if (!foundGetter) {
                    addGetterMethod();
                }
                if (!foundSetter) {
                    addSetterMethod();
                }
                
                super.visitEnd();
            }

            private void addInjectedField() {
                cv.visitField(
                    Opcodes.ACC_PRIVATE | Opcodes.ACC_VOLATILE,
                    IBithonObject.INJECTED_FIELD_NAME,
                    OBJECT_DESCRIPTOR,
                    null,
                    null);
            }

            private void addGetterMethod() {
                final MethodVisitor mv = cv.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    GETTER_METHOD,
                    GETTER_METHOD_DESCRIPTOR,
                    null,
                    null);

                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(
                    Opcodes.GETFIELD,
                    instrumentedName,
                    IBithonObject.INJECTED_FIELD_NAME,
                    OBJECT_DESCRIPTOR);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }

            private void addSetterMethod() {
                final MethodVisitor mv = cv.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    SETTER_METHOD,
                    SETTER_METHOD_DESCRIPTOR,
                    null,
                    null);

                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitFieldInsn(
                    Opcodes.PUTFIELD,
                    instrumentedName,
                    IBithonObject.INJECTED_FIELD_NAME,
                    OBJECT_DESCRIPTOR);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        };
    }
}
