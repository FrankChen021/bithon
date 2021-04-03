package com.sbss.bithon.agent.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
    final List<JarFile> jars;

    JarClassLoader(File directory, ClassLoader parent) {
        super(parent);
        this.jars = JarResolver.resolve(directory);
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new ClassNotFoundException("Can't find " + name);
    }

    @Override
    public URL getResource(String name) {
        for (JarFile jarFile : jars) {
            JarEntry entry = jarFile.getJarEntry(name);
            if (entry != null) {
                try {
                    return JarUtils.getClassURL(jarFile, name);
                } catch (IOException e) {
                    // resource didn't exist in current jarFile, search the next one
                }
            }
        }
        return super.getResource(name);
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
}
