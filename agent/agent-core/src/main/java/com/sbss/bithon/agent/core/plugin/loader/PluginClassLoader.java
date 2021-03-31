package com.sbss.bithon.agent.core.plugin.loader;

import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;

/**
 * Class loader for agent to load plugins
 *
 * @author frankchen
 * @date 2020-12-31 22:25:45
 */
public class PluginClassLoader extends ClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(PluginClassLoader.class);

    private static ClassLoader agentClassLoader;
    private static final List<JarFileItem> SEARCH_JARS = new ArrayList<>();
    private static PluginClassLoader DEFAULT_INSTANCE;

    PluginClassLoader(ClassLoader parent) {
        super(new ChainedClassLoader(agentClassLoader, parent));
    }

    public static PluginClassLoader createInstance(ClassLoader agentClassLoader) {
        PluginClassLoader.agentClassLoader = agentClassLoader;
        DEFAULT_INSTANCE = new PluginClassLoader(Thread.currentThread().getContextClassLoader());
        AgentClassloaderManager.register(Thread.currentThread().getContextClassLoader(), DEFAULT_INSTANCE);
        return DEFAULT_INSTANCE;
    }

    public static ClassLoader getAgentClassLoader() {
        return agentClassLoader;
    }

    public static ClassLoader getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static void appendSearchFiles(List<JarFileItem> files) {
        SEARCH_JARS.addAll(files);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String path = name.replace('.', '/').concat(".class");
        for (JarFileItem jarFile : SEARCH_JARS) {
            JarEntry entry = jarFile.jarFile.getJarEntry(path);
            if (entry == null) {
                continue;
            }

            try {
                byte[] classBytes = jarFile.openClassFile(path);
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException e) {
                logger.error("find class fail.", e);
            }
        }
        throw new ClassNotFoundException("Can't find " + name);
    }

    @Override
    public URL getResource(String name) {
        for (JarFileItem jarFile : SEARCH_JARS) {
            JarEntry entry = jarFile.jarFile.getJarEntry(name);
            if (entry != null) {
                try {
                    return new URL("jar:file:" + jarFile.sourceFile.getAbsolutePath() + "!/" + name);
                } catch (MalformedURLException e) {
                    // resource didn't exist in current jarFile, search the next one
                }
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allResources = new LinkedList<>();
        for (JarFileItem jarFile : SEARCH_JARS) {
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
