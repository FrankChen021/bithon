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

package org.bithon.agent.instrumentation.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/3 12:20
 */
public class JarClassLoader extends ClassLoader {
    private final String name;
    private final List<Jar> jars;
    private final IClassLoaderProvider[] parents;

    public interface IClassLoaderProvider {
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

    private static class ClassByteCode {
        final Jar jar;
        final byte[] byteCode;

        ClassByteCode(byte[] byteCode, Jar jar) {
            this.byteCode = byteCode;
            this.jar = jar;
        }
    }

    public JarClassLoader(String name, File directory, ClassLoader... parents) {
        this(name,
             JarResolver.resolve(directory),
             Arrays.stream(parents)
                   .map(ClassLoaderProvider::new)
                   .toArray(IClassLoaderProvider[]::new));
    }

    /**
     * @param name used for logging
     */
    public JarClassLoader(String name,
                          List<Jar> jars,
                          IClassLoaderProvider... parents) {
        // NOTE: parent is assigned to parent class loader
        // This is the key to implement agent lib isolation from app libs
        super(null);
        this.name = name;
        this.jars = jars;
        this.parents = parents;
    }

    public List<Jar> getJars() {
        return jars;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            ClassByteCode byteCode = getClassByteCode(name);
            if (byteCode != null) {
                // define the package object for customer-loaded classes,
                // so that getPackage could work
                int lastIndex = name.lastIndexOf(".");
                if (lastIndex > 0) {
                    String pkg = name.substring(0, lastIndex);
                    if (getPackage(pkg) == null) {
                        definePackage(pkg, null, null, null, null, null, null, null);
                    }
                }

                // Create CodeSource with the JAR file location
                // For Java agents, we use null permissions to inherit from the security policy
                // This allows agent code to run with appropriate privileges while maintaining
                // proper CodeSource information for debugging and introspection
                CodeSource codeSource = new CodeSource(byteCode.jar.getURL(), (java.security.cert.Certificate[]) null);

                // Create ProtectionDomain with the CodeSource
                // Passing null for permissions means the security policy will be consulted
                // The classloader reference ensures proper security context
                ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, null, this, null);

                return defineClass(name, byteCode.byteCode, 0, byteCode.byteCode.length, protectionDomain);
            }
        } catch (IOException ignored) {
        }

        for (IClassLoaderProvider parent : parents) {
            try {
                ClassLoader parentLoader = parent.getClassLoader();
                if (parentLoader != this) {
                    // parent is a provider, it could be set dynamically to be instanced of current class
                    return parentLoader.loadClass(name);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(String.format(Locale.ENGLISH,
                                                       "%s not found in %s, parents:%s",
                                                       name,
                                                       this.name,
                                                       Arrays.stream(this.parents)
                                                             .map(p -> p.getClassLoader().getClass().getName())
                                                             .collect(Collectors.joining(","))));
    }

    @Override
    public URL getResource(String name) {
        for (Jar jar : jars) {
            JarEntry entry = jar.getJarFile().getJarEntry(name);
            if (entry == null) {
                continue;
            }
            try {
                return JarUtils.getClassURL(jar.getJarFile(), name);
            } catch (IOException ignored) {
                // resource didn't exist in the current jarFile, search the next one
            }
        }

        // delegate to parent to get resource
        for (IClassLoaderProvider parent : parents) {
            ClassLoader parentLoader = parent.getClassLoader();
            if (parentLoader != this) {
                URL url = parentLoader.getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allResources = new LinkedList<>();
        for (Jar jar : jars) {
            JarEntry entry = jar.getJarFile().getJarEntry(name);
            if (entry != null) {
                allResources.add(JarUtils.getClassURL(jar.getJarFile(), name));
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

    private ClassByteCode getClassByteCode(String name) throws IOException {
        String path = name.replace('.', '/').concat(".class");
        for (Jar jar : jars) {
            byte[] byteCode = jar.getClassByteCode(path);
            if (byteCode != null) {
                return new ClassByteCode(byteCode, jar);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
