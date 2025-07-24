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

package org.bithon.agent.instrumentation.bytecode;

import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassReader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassWriter;
import org.bithon.shaded.net.bytebuddy.jar.asm.commons.ClassRemapper;
import org.bithon.shaded.net.bytebuddy.jar.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Copy the bytecode of a class into a new class,
 * and package renaming(including all references) are supported.
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/25 17:52
 */
public class ClassCopier {
    static class Pair {
        /**
         * slash-separated full qualified name.
         * e.g. org/bithon/agent/core/shading/ClassShader
         */
        String targetClazzName;

        /**
         * slash-separated full qualified name.
         * e.g. org/bithon/agent/core/shading/ClassShader
         */
        String sourceClassName;

        public Pair(String originalClass, String newClassName) {
            this.targetClazzName = newClassName;
            this.sourceClassName = originalClass;
        }
    }

    /**
     * a map that holds the name mapping
     */
    private final Map<String, Pair> shadedNames = new HashMap<>();

    private String oldPackage;
    private String newPackage;

    public ClassCopier changePackage(String oldPackage, String newPackage) {
        this.oldPackage = oldPackage.replace('.', '/');
        this.newPackage = newPackage.replace('.', '/');
        return this;
    }

    /**
     * Replace all references to the {@link #oldPackage} in the {@code oldClass} to the {@link #newPackage}
     */
    public ClassCopier copyClass(String oldClass, String newClass) throws IOException {
        oldClass = oldClass.replace('.', '/');
        shadedNames.put(oldClass, new Pair(oldClass, newClass.replace('.', '/')));
        return this;
    }

    /**
     * @param classLoader The class loader that the original classes can be loaded and the target class will be injected into.
     */
    public void to(ClassLoader classLoader) throws IOException {
        Map<String, byte[]> shadedClasses = new HashMap<>(3);

        for (Map.Entry<String, Pair> entry : this.shadedNames.entrySet()) {
            Pair val = entry.getValue();

            byte[] classInBytes = copy(val.sourceClassName, classLoader);
            if (classInBytes == null) {
                LoggerFactory.getLogger(ClassCopier.class)
                             .error("Can't copy class from [{}] to [{}]: source class not found", val.sourceClassName, val.targetClazzName);
                continue;
            }

            // save for further injection
            shadedClasses.put(val.targetClazzName, classInBytes);

            InstrumentationHelper.getAopDebugger()
                                 .writeTo(val.targetClazzName, classInBytes);
        }

        //
        // Inject into target class loader
        //
        ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance());
        factory.make(classLoader, null).injectRaw(shadedClasses);
    }

    private byte[] copy(String clazz, ClassLoader classLoader) throws IOException {
        String classFile = clazz + ".class";
        try (InputStream clazzByteStream = classLoader.getResourceAsStream(classFile)) {
            if (clazzByteStream == null) {
                return null;
            }

            ClassReader clazzReader = new ClassReader(clazzByteStream);
            ClassWriter clazzWriter = new ClassWriter(0);
            clazzReader.accept(new ClassRemapper(clazzWriter, new Remapper() {
                @Override
                public String mapType(String internalName) {
                    Pair shaded = shadedNames.get(internalName);
                    if (shaded != null) {
                        return shaded.targetClazzName;
                    } else {
                        return super.mapType(internalName);
                    }
                }

                @Override
                public String map(String internalName) {
                    if (oldPackage != null && newPackage != null) {
                        return internalName.replace(oldPackage, newPackage);
                    } else {
                        return super.map(internalName);
                    }
                }
            }), ClassReader.EXPAND_FRAMES);

            return clazzWriter.toByteArray();
        }
    }
}
