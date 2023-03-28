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

package org.bithon.agent.instrumentation.aop.interceptor;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.agent.instrumentation.loader.JarClassLoader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassReader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/19 15:45
 */
public class InterceptorTypeResolver {

    private final JarClassLoader classLoader;
    private final Map<String, InterceptorType> superType = new HashMap<>();

    public InterceptorTypeResolver(JarClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public InterceptorType resolve(String name) {
        SuperNameExtractor visitor = new SuperNameExtractor();

        try (InputStream is = classLoader.getClassStream(name)) {
            ClassReader cr = new ClassReader(is);
            cr.accept(visitor, ClassReader.SKIP_FRAMES);
        } catch (IOException ignored) {
            throw new AgentException("Can't resolve class [%s]", name);
        }

        String superName = visitor.getSuperName();
        if (InterceptorType.AROUND.type().equals(superName)) {
            return InterceptorType.AROUND;
        } else if (InterceptorType.BEFORE.type().equals(superName)) {
            return InterceptorType.BEFORE;
        } else if (InterceptorType.AFTER.type().equals(superName)) {
            return InterceptorType.AFTER;
        } else if (InterceptorType.REPLACEMENT.type().equals(superName)) {
            return InterceptorType.REPLACEMENT;
        } else {
            InterceptorType type = superType.get(superName);
            if (type != null) {
                return type;
            }
            type = resolve(superName);
            superType.put(superName, type);
            return type;
        }
    }

    static class SuperNameExtractor extends ClassVisitor {
        private String superName;

        protected SuperNameExtractor() {
            super(Opcodes.ASM9);
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
    }
}
