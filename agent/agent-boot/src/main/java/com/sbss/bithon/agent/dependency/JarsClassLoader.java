package com.sbss.bithon.agent.dependency;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/3 12:20
 */
class JarsClassLoader extends ClassLoader {
    final List<JarFileItem> jars;

    JarsClassLoader(File directory, ClassLoader parent) {
        super(parent);
        this.jars = JarFileResolver.resolve(directory);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        for (JarFileItem jarFile : jars) {
            JarEntry entry = jarFile.jarFile.getJarEntry(path);
            if (entry == null) {
                continue;
            }

            try {
                byte[] classBytes = jarFile.openClassFile(path);
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new ClassNotFoundException("Can't find " + name);
    }

    @Override
    public URL getResource(String name) {
        for (JarFileItem jarFile : jars) {
            JarEntry entry = jarFile.jarFile.getJarEntry(name);
            if (entry != null) {
                try {
                    return new URL("jar:file:" + jarFile.sourceFile.getAbsolutePath() + "!/" + name);
                } catch (MalformedURLException e) {
                    // resource didn't exist in current jarFile, search the next one
                }
            }
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allResources = new LinkedList<>();
        for (JarFileItem jarFile : jars) {
            JarEntry entry = jarFile.jarFile.getJarEntry(name);
            if (entry != null) {
                allResources.add(new URL("jar:file:" + jarFile.sourceFile.getAbsolutePath() + "!/" + name));
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
