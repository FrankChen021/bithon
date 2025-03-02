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

package org.bithon.agent.rpc.brpc.cmd;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/12 20:12
 */
public class ClassDisassembler {

    static class MememberComparator implements Comparator<Member> {
        @Override
        public int compare(Member lhs, Member rhs) {
            int result = Boolean.compare(Modifier.isStatic(lhs.getModifiers()),
                                         Modifier.isStatic(rhs.getModifiers()));
            if (result != 0) {
                return result < 0 ? 1 : -1;
            }

            // Then by access modifiers
            result = getModifierOrder(rhs.getModifiers()) - getModifierOrder(lhs.getModifiers());
            if (result != 0) {
                return result;
            }

            return lhs.getName().compareTo(rhs.getName());
        }

        private int getModifierOrder(int modifiers) {
            if (Modifier.isPublic(modifiers)) {
                return 3;
            }
            if (Modifier.isProtected(modifiers)) {
                return 2;
            }
            if (Modifier.isPrivate(modifiers)) {
                return 1;
            }
            return 0;
        }
    }

    public static String toModifier(int modifier) {
        StringBuilder s = new StringBuilder();
        if (Modifier.isPublic(modifier)) {
            s.append("public ");
        }
        if (Modifier.isProtected(modifier)) {
            s.append("protected ");
        }
        if (Modifier.isPrivate(modifier)) {
            s.append("private ");
        }
        if (Modifier.isFinal(modifier)) {
            s.append("final ");
        }
        if (Modifier.isSynchronized(modifier)) {
            s.append("synchronized ");
        }
        if (Modifier.isAbstract(modifier)) {
            s.append("abstract ");
        }
        if (Modifier.isStatic(modifier)) {
            s.append("static ");
        }
        if (Modifier.isTransient(modifier)) {
            s.append("transient ");
        }
        if (Modifier.isVolatile(modifier)) {
            s.append("volatile ");
        }
        if (Modifier.isNative(modifier)) {
            s.append("native ");
        }

        return s.toString();
    }

    private final Set<String> imports = new TreeSet<>();
    private final StringBuilder body = new StringBuilder();
    private final Class<?> clazz;

    public ClassDisassembler(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String disassemble() {
        outputAnnotations("", clazz.getDeclaredAnnotations());
        outputModifier(clazz.getModifiers());
        body.append("class ");
        body.append(clazz.getSimpleName());
        outputSuperClass();
        outputImplements();
        body.append(" {");
        outputFields();
        outputConstructors();
        outputMethods();
        body.append("\n}");

        StringBuilder unit = new StringBuilder(1024);
        formatPackage(unit);

        for (String imp : imports) {
            unit.append("import ");
            unit.append(imp);
            unit.append(';');
            unit.append('\n');
        }
        if (!imports.isEmpty()) {
            unit.append('\n');
        }
        unit.append(body);

        return unit.toString();
    }

    private void formatPackage(StringBuilder sb) {
        String pkg = clazz.getPackage().getName();
        if (!pkg.isEmpty()) {
            sb.append("package ");
            sb.append(pkg);
            sb.append(";\n\n");
        }
    }

    private void outputModifier(int modifier) {
        String m = toModifier(modifier);
        if (!m.isEmpty()) {
            body.append(m);
        }
    }

    private void outputSuperClass() {
        Class<?> parent = clazz.getSuperclass();
        if (parent != null && !parent.equals(Object.class)) {
            body.append(" extends ");
            formatClassName(parent);
        }
    }

    private void outputImplements() {
        Class<?>[] implementsList = this.clazz.getInterfaces();
        if (implementsList.length > 0) {
            body.append(" implements ");
            for (int i = 0; i < implementsList.length; i++) {
                if (i != 0) {
                    body.append(", ");
                }
                formatClassName(implementsList[i]);
            }
        }
    }

    private void outputFields() {
        Field[] fields = Arrays.stream(clazz.getDeclaredFields())
                               .sorted(new MememberComparator())
                               .toArray(Field[]::new);

        for (Field field : fields) {
            body.append('\n');
            outputAnnotations("\t", field.getDeclaredAnnotations());

            body.append('\t');
            outputModifier(field.getModifiers());
            formatClassName(field.getGenericType());
            body.append(' ');
            body.append(field.getName());
            body.append(';');
        }
    }

    private void outputConstructors() {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors.length != 0) {
            // Extra newline acting as block separator
            body.append('\n');
        }
        for (Constructor<?> ctor : ctors) {
            body.append('\n');

            outputAnnotations("\t", ctor.getDeclaredAnnotations());

            body.append('\t');
            outputModifier(ctor.getModifiers());
            body.append(clazz.getSimpleName());
            formatParameters(ctor.getParameters());
            body.append(';');
        }
    }

    private void outputMethods() {
        Method[] methodList = Arrays.stream(clazz.getDeclaredMethods())
                                    .sorted(new MememberComparator())
                                    .toArray(Method[]::new);

        if (methodList.length != 0) {
            body.append('\n');
        }
        for (Method method : methodList) {
            body.append('\n');

            outputAnnotations("\t", method.getDeclaredAnnotations());

            body.append('\t');
            outputModifier(method.getModifiers());
            formatClassName(method.getReturnType());

            body.append(' ');
            body.append(method.getName());
            formatParameters(method.getParameters());
            body.append(';');
        }
    }

    private void formatParameters(Parameter[] parameters) {
        body.append('(');
        for (int j = 0; j < parameters.length; j++) {
            if (j != 0) {
                body.append(", ");
            }
            formatClassName(parameters[j].getType());
            body.append(' ');
            body.append(parameters[j].getName());
        }
        body.append(')');
    }

    private void outputAnnotations(String indent, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (!indent.isEmpty()) {
                body.append(indent);
            }
            body.append('@');
            formatClassName(annotation.annotationType());
            body.append('\n');
        }
    }

    private void formatClassName(Type type) {
        if (type instanceof ParameterizedType) {
            formatClassName(((ParameterizedType) type).getRawType());
            body.append('<');
            Type[] typeArgs = ((ParameterizedType) type).getActualTypeArguments();
            for (int i = 0; i < typeArgs.length; i++) {
                if (i != 0) {
                    body.append(", ");
                }
                formatClassName(typeArgs[i]);
            }
            body.append('>');
            return;
        }
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;

            if (clazz.isArray()) {
                formatClassName(clazz.getComponentType());
                body.append("[]");
                return;
            }

            String name = clazz.getName();
            body.append(clazz.getSimpleName());

            boolean defaultImport = (name.startsWith("java.lang.")
                                     // No sub packages under the java.lang.
                                     && name.indexOf('.', "java.lang.".length()) == -1);
            if (!clazz.isPrimitive() && !defaultImport) {
                imports.add(clazz.getName());
            }
            return;
        }
        if (type instanceof WildcardType) {
            body.append(type);
            return;
        }
        body.append("NOT SUPPORTED: ").append(type.getTypeName());
    }

    public static void main(String[] args) {
        System.out.println(new ClassDisassembler(ClassDisassembler.class).disassemble());
    }
}
