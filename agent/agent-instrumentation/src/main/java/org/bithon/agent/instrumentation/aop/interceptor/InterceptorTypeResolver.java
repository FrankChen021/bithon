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

import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.ReplaceInterceptor;
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
 * Read the class file and determine which base interceptor the class inherits from
 *
 * @author frank.chen021@outlook.com
 * @date 2023/3/19 15:45
 */
public class InterceptorTypeResolver {

    private final String AROUND = AroundInterceptor.class.getName().replace('.', '/');
    private final String BEFORE = BeforeInterceptor.class.getName().replace('.', '/');
    private final String AFTER = AfterInterceptor.class.getName().replace('.', '/');
    private final String REPLACE = ReplaceInterceptor.class.getName().replace('.', '/');

    private final JarClassLoader classLoader;
    private final Map<String, InterceptorType> superType = new HashMap<>();

    public InterceptorTypeResolver(JarClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Resolve the interceptor type by finding its superclass declaration from the class bytecode.
     * The ASM is used to read & process bytecode.
     */
    public InterceptorType resolve(String interceptorClassName) {
        SuperNameExtractor extractor = new SuperNameExtractor();

        try (InputStream is = classLoader.getClassStream(interceptorClassName)) {
            ClassReader cr = new ClassReader(is);
            cr.accept(extractor, ClassReader.SKIP_FRAMES);
        } catch (IOException ignored) {
            throw new AgentException("Can't resolve class [%s]", interceptorClassName);
        }

        String superName = extractor.getSuperName();
        if (AROUND.equals(superName)) {
            return InterceptorType.AROUND;
        } else if (BEFORE.equals(superName)) {
            return InterceptorType.BEFORE;
        } else if (AFTER.equals(superName)) {
            return InterceptorType.AFTER;
        } else if (REPLACE.equals(superName)) {
            return InterceptorType.REPLACEMENT;
        } else {
            // The direct superclass is not the default one,
            // this means that the superclass might be derived from the default one,
            // we need to recursively resolve the superclass.
            //
            // And the superType here is just a cache for any resolved type for a given superclass.
            InterceptorType type = superType.get(superName);
            if (type != null) {
                return type;
            }
            type = resolve(superName);
            superType.put(superName, type);
            return type;
        }
    }

    /**
     * Find the direct superclass
     */
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
