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

import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassReader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassWriter;
import org.bithon.shaded.net.bytebuddy.jar.asm.commons.ClassRemapper;
import org.bithon.shaded.net.bytebuddy.jar.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/25 17:52
 */
public class ClassShader {
    private final String oldPackage;
    private final String newPackage;

    public ClassShader(String oldPackage, String newPackage) {
        this.oldPackage = oldPackage.replace('.', '/');
        this.newPackage = newPackage.replace('.', '/');
    }

    public void shade(Class<?> rawClass, String targetName, ClassLoader targetClassLoader) throws IOException {
        byte[] bytes = shade(rawClass);

        // inject into target class loader
        ClassInjector.UsingUnsafe.Factory factory = ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance());
        factory.make(targetClassLoader, null).injectRaw(Collections.singletonMap(targetName, bytes));
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
                public String map(String internalName) {
                    return internalName.replace(oldPackage, newPackage);
                }
            }), ClassReader.EXPAND_FRAMES);

            return clazzWriter.toByteArray();
        }
    }
}
