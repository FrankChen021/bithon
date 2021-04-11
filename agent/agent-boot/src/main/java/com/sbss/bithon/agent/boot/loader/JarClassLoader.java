/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.boot.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/3 12:20
 */
public class JarClassLoader extends ClassLoader {
    private final String name;
    private final List<JarFile> jars;
    private final IClassLoaderProvider[] parents;

    interface IClassLoaderProvider {
        ClassLoader getClassLoader();
    }

    static class ClassLoaderProvider implements IClassLoaderProvider {
        private final ClassLoader classLoader;

        ClassLoaderProvider(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        @Override
        public ClassLoader getClassLoader() {
            return classLoader;
        }
    }

    public JarClassLoader(String name, List<JarFile> jars, ClassLoader... parents) {
        this(name, jars, Arrays.stream(parents).map(ClassLoaderProvider::new).toArray(IClassLoaderProvider[]::new));
    }

    public JarClassLoader(String name, List<JarFile> jars, IClassLoaderProvider... parents) {
        // NOTE:  parent is assigned to parent class loader
        // This is the key to implement agent lib isolation from app libs
        super(null);
        this.name = name;
        this.jars = jars;
        this.parents = parents;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        for (JarFile jarFile : jars) {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry == null) {
                continue;
            }

            try {
                byte[] classBytes = JarUtils.openClassFile(jarFile, path);
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException ignored) {
            }
        }

        for (IClassLoaderProvider parent : parents) {
            try {
                return parent.getClassLoader().loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        for (JarFile jarFile : jars) {
            JarEntry entry = jarFile.getJarEntry(name);
            if (entry == null) {
                continue;
            }
            try {
                return JarUtils.getClassURL(jarFile, name);
            } catch (IOException ignored) {
                // resource didn't exist in current jarFile, search the next one
            }
        }

        // delegate to parent to get resource
        for (IClassLoaderProvider parent : parents) {
            URL url = parent.getClassLoader().getResource(name);
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allResources = new LinkedList<>();
        for (JarFile jarFile : jars) {
            JarEntry entry = jarFile.getJarEntry(name);
            if (entry != null) {
                allResources.add(JarUtils.getClassURL(jarFile, name));
            }
        }

        final Iterator<URL> iterator = allResources.iterator();
        return new Enumeration<URL>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return iterator.next();
            }
        };
    }

    public List<JarFile> getJars() {
        return Collections.unmodifiableList(this.jars);
    }

    @Override
    public String toString() {
        return name;
    }
}
