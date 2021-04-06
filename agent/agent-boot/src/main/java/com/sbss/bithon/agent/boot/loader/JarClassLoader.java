package com.sbss.bithon.agent.boot.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
class JarClassLoader extends ClassLoader {
    private final List<JarFile> jars;
    private final ClassLoader parent;

    JarClassLoader(File directory, ClassLoader parent) {
        // NOTE:  parent is assigned to parent class loader
        // This is the key to implement agent lib isolation from app libs
        super(null);
        this.jars = JarResolver.resolve(directory);
        this.parent = parent;
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

        // delegate to parent to load class
        return parent.loadClass(name);
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

        // delegate to parent to get resources
        return parent.getResource(name);
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
}
