package com.sbss.bithon.agent.core.loader;

import com.sbss.bithon.agent.core.Constant;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.io.File.separator;

/**
 * Description : Agent自己的classloader, 用于动态加载plugin, parent为切点classloader,
 * 以保证所有引用类型(class type)对插件可见, 可以添加agent自身的搜索路径 <br>
 * Date: 17/11/30
 *
 * @author 马至远
 */
public class AgentClassloader extends ClassLoader {
    private static final Logger logger = LoggerFactory.getLogger(AgentClassloader.class);

    /**
     * classloader 需要搜索的路径
     */
    private List<File> classloaderSearchLocations;

    /**
     * 存储依赖位置
     */
    private List<Jar> jars;

    /**
     * jar加载过程需要加锁, 避免冲突
     */
    private ReentrantLock jarScanLock = new ReentrantLock();

    private static AgentClassloader DEFAULT;

    /**
     * 获取默认的AgentClassloader实例, 此实例用于加载基本plugin配置信息, 单例
     */
    public static AgentClassloader getDefaultInstance() {
        if (DEFAULT == null) {
            // 使用当前classloader作为父类
            DEFAULT = new AgentClassloader(Thread.currentThread().getContextClassLoader());
            AgentClassloaderManager.register(Thread.currentThread().getContextClassLoader(), DEFAULT);
        }
        return DEFAULT;
    }

    AgentClassloader(ClassLoader parent) {
        super(parent);
        classloaderSearchLocations = new LinkedList<>();
        File pluginsPath = findPluginsLocation();
        classloaderSearchLocations.add(pluginsPath);
        resolveJars();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // 搜索路径中的所有jar文件
        String path = name.replace('.', '/').concat(".class");
        for (Jar jar : jars) {
            JarEntry entry = jar.jarFile.getJarEntry(path);
            if (entry == null)
                continue;

            try {
                URL classFileUrl = new URL("jar:file:" + jar.sourceFile.getAbsolutePath() + "!/" + path);
                byte[] data;
                BufferedInputStream is = null;
                ByteArrayOutputStream baos = null;
                try {
                    is = new BufferedInputStream(classFileUrl.openStream());
                    baos = new ByteArrayOutputStream();
                    int ch;
                    while ((ch = is.read()) != -1) {
                        baos.write(ch);
                    }
                    data = baos.toByteArray();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (baos != null) {
                        try {
                            baos.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                return defineClass(name, data, 0, data.length);
            } catch (IOException e) {
                logger.error("find class fail.", e);
            }
        }
        throw new ClassNotFoundException("Can't find " + name);
    }

    @Override
    public URL getResource(String name) {
        for (Jar jar : jars) {
            JarEntry entry = jar.jarFile.getJarEntry(name);
            if (entry != null) {
                try {
                    return new URL("jar:file:" + jar.sourceFile.getAbsolutePath() + "!/" + name);
                } catch (MalformedURLException e) {
                    // resource didn't exist in current jar, search the next one
                }
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> allResources = new LinkedList<>();
        for (Jar jar : jars) {
            JarEntry entry = jar.jarFile.getJarEntry(name);
            if (entry != null) {
                allResources.add(new URL("jar:file:" + jar.sourceFile.getAbsolutePath() + "!/" + name));
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

    /**
     * 搜索plugin所在文件夹位置
     *
     * @return plugin file
     */
    private File findPluginsLocation() {
        return new File(AgentClassloaderManager.getAgentPath() + separator + Constant.PLUGIN_DIR);
    }

    /**
     * 解析当前classpathLocation下所有资源文件
     */
    private void resolveJars() {
        if (jars != null) {
            return;
        }

        jarScanLock.lock();
        if (jars != null) {
            //double check
            jarScanLock.unlock();
            return;
        }

        try {
            jars = new LinkedList<>();
            for (File path : classloaderSearchLocations) {
                if (path.exists() && path.isDirectory()) {
                    String[] jarFileNames = path.list((dir,
                                                       name) -> name.endsWith(".jar"));

                    assert jarFileNames != null;
                    for (String fileName : jarFileNames) {
                        try {
                            File file = new File(path, fileName);
                            Jar jar = new Jar(new JarFile(file), file);
                            jars.add(jar);
                        } catch (IOException e) {
                            logger.error("{} jar file can't be resolved", fileName, e);
                        }
                    }
                }
            }
        } finally {
            jarScanLock.unlock();
        }
    }

    private final class Jar {
        private JarFile jarFile;
        private File sourceFile;

        private Jar(JarFile jarFile, File sourceFile) {
            this.jarFile = jarFile;
            this.sourceFile = sourceFile;
        }
    }
}
