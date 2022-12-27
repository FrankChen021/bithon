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

package org.bithon.agent.core.shading;

import org.bithon.agent.core.aop.AopDebugger;
import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassReader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassWriter;
import org.bithon.shaded.net.bytebuddy.jar.asm.commons.ClassRemapper;
import org.bithon.shaded.net.bytebuddy.jar.asm.commons.Remapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Rename the package name of all class references in a specified class to another package name, and then create a new class.
 * It's like the 'mvn shade plugin'.
 *
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/25 17:52
 */
public class ClassShader {
    private final String oldPackage;
    private final String newPackage;

    static class Pair {
        /**
         * slash-separated full qualified name.
         * e.g. org/bithon/agent/core/shading/ClassShader
         */
        String newClazzName;
        Class<?> originalClass;

        public Pair(Class<?> originalClass, String newClassName) {
            this.newClazzName = newClassName;
            this.originalClass = originalClass;
        }
    }

    /**
     * a map that holds the name mapping
     */
    private final Map<String, Pair> shadedNames = new HashMap<>();

    /**
     * a map that holds the result
     */


    public ClassShader(String oldPackage, String newPackage) {
        this.oldPackage = oldPackage.replace('.', '/');
        this.newPackage = newPackage.replace('.', '/');
    }

    /**
     * Replace all references to the {@link #oldPackage} in the {@code originalClazz} to the {@link #newPackage}
     */
    public ClassShader add(Class<?> originalClazz, String newClazzName) throws IOException {
        shadedNames.put(originalClazz.getName().replace('.', '/'), new Pair(originalClazz, newClazzName.replace('.', '/')));
        return this;
    }

    public void shade(ClassLoader targetClassLoader) throws IOException {
        Map<String, byte[]> shadedClasses = new HashMap<>(3);

        for (Map.Entry<String, Pair> entry : this.shadedNames.entrySet()) {
            Pair val = entry.getValue();

            byte[] classInBytes = shade(val.originalClass);
            shadedClasses.put(val.newClazzName, classInBytes);

            if (AopDebugger.IS_DEBUG_ENABLED) {
                try {
                    try (FileOutputStream output = new FileOutputStream(new File(AopDebugger.CLASS_FILE_DIR, val.newClazzName.replace('/', '.') + ".class"))) {
                        output.write(classInBytes);
                    }
                } catch (IOException ignored) {
                }
            }
        }

        //
        // Inject into target class loader
        //
        ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance());
        factory.make(targetClassLoader, null).injectRaw(shadedClasses);
    }

    private byte[] shade(Class<?> clazz) throws IOException {
        String classFile = clazz.getName().replace('.', '/') + ".class";
        try (InputStream clazzByteStream = clazz.getClassLoader().getResourceAsStream(classFile)) {
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
                        return shaded.newClazzName;
                    } else {
                        return super.mapType(internalName);
                    }
                }

                @Override
                public String map(String internalName) {
                    return internalName.replace(oldPackage, newPackage);
                }
            }), ClassReader.EXPAND_FRAMES);

            return clazzWriter.toByteArray();
        }
    }
}
