package com.sbss.bithon.agent.core.plugin.loader;

import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    ClassLoader[] parents;

    private PluginClassLoader(ClassLoader... parents) {
        this.parents = parents;
    }

    public static PluginClassLoader createClassLoader(ClassLoader parentClassLoader) {
        return new PluginClassLoader(agentClassLoader, parentClassLoader);
    }

    public static PluginClassLoader setAgentClassLoader(ClassLoader agentClassLoader) {
        PluginClassLoader.agentClassLoader = agentClassLoader;
        DEFAULT_INSTANCE = createClassLoader(Thread.currentThread().getContextClassLoader());
        PluginClassLoaderManager.register(Thread.currentThread().getContextClassLoader(), DEFAULT_INSTANCE);
        return DEFAULT_INSTANCE;
    }
    // TODO： move into PluginClassLoaderManager
    public static ClassLoader getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (int i = 0; i < parents.length; i++) {
            try {
                return parents[i].loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name);
    }

//    public static void appendSearchFiles(List<JarFileItem> files) {
//        SEARCH_JARS.addAll(files);
//    }
//
//    @Override
//    protected Class<?> findClass(String name) throws ClassNotFoundException {
//        String path = name.replace('.', '/').concat(".class");
//        for (JarFileItem jarFile : SEARCH_JARS) {
//            JarEntry entry = jarFile.jarFile.getJarEntry(path);
//            if (entry == null) {
//                continue;
//            }
//
//            try {
//                byte[] classBytes = jarFile.openClassFile(path);
//                return defineClass(name, classBytes, 0, classBytes.length);
//            } catch (IOException e) {
//                logger.error("find class fail.", e);
//            }
//        }
//        throw new ClassNotFoundException("Can't find " + name);
//    }
//
    @Override
    public URL getResource(String name) {
        for (int i = 0; i < parents.length; i++) {
            URL url = parents[i].getResource(name);
            if ( url != null ) {
                return url;
            }
        }
        return null;
    }
//
//    @Override
//    public Enumeration<URL> getResources(String name) throws IOException {
//        List<URL> allResources = new LinkedList<>();
//        for (JarFileItem jarFile : SEARCH_JARS) {
//            JarEntry entry = jarFile.jarFile.getJarEntry(name);
//            if (entry != null) {
//                allResources.add(new URL("jar:file:" + jarFile.sourceFile.getAbsolutePath() + "!/" + name));
//            }
//        }
//
//        final Iterator<URL> iterator = allResources.iterator();
//        return new Enumeration<URL>() {
//            @Override
//            public boolean hasMoreElements() {
//                return iterator.hasNext();
//            }
//
//            @Override
//            public URL nextElement() {
//                return iterator.next();
//            }
//        };
//    }
}
