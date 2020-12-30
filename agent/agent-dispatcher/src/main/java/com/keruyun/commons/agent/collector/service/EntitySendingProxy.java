package com.keruyun.commons.agent.collector.service;

import com.keruyun.commons.agent.collector.service.AgentService.Client;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * @author lizheng
 */
public class EntitySendingProxy {

    /**
     * 匹配结果集
     * key: 消息类全路径名称
     * value: 关联方法
     */
    private static final Map<String, Method> CLASS_TO_METHOD = new HashMap<>();

    private static final String BASE_PACKAGE = "com.keruyun.commons.agent.collector.entity";

    private static final String PROTOCOL_JAR = "jar";

    private static final String FILE_PREFIX = "file:/";

    private static final String JAR_PATH = "!/";

    private static final String PATH_FLAG = "/";

    private static final String DOT = ".";

    private static final String SEND_METHOD_PREFIX = "write";

    public EntitySendingProxy() {
        List<String> classNames = findEntityClassNames();
        List<Method> methods = findSendMethods();
        
        //在writeXXX方法中找到接收参数类型为Entity的方法
        classNames.forEach(className -> methods.stream()
                                       .filter(method -> className.equals(method.getParameterTypes()[0].getName()))
                                       .findFirst()
                                       .ifPresent(method -> CLASS_TO_METHOD.put(className, method)));
    }

    public String send(Client client, Object entity) throws Exception {
        String result = null;
        String className = entity.getClass().getName();
        Method method = CLASS_TO_METHOD.get(className);
        if (null == method) {
            System.err.println("No service method found for entity: " + className);
        } else {
            result = (String) method.invoke(client, entity);
        }
        return result;
    }

    private List<String> findEntityClassNames() {
        List<String> result = new ArrayList<>();
        String packagePath = BASE_PACKAGE.replace(DOT, PATH_FLAG);
        URL url = Thread.currentThread().getContextClassLoader().getResource(packagePath);
        if (url != null && PROTOCOL_JAR.equals(url.getProtocol())) {
            String fixedJarPath = url.getFile()
                    .replace(FILE_PREFIX, "")
                    .replace(JAR_PATH + packagePath, "");
            if (!fixedJarPath.startsWith(PATH_FLAG)) {
                fixedJarPath = PATH_FLAG + fixedJarPath;
            }
            boolean isNestedJar = fixedJarPath.contains(JAR_PATH);
            try {
                JarFile jarFile;
                if (isNestedJar) {
                    String[] values = fixedJarPath.split(JAR_PATH);
                    jarFile = new JarFile(values[0]);
                    JarEntry rpcEntry = new JarEntry(values[1]);
                    result = fetchNestedEntityClassNames(jarFile, rpcEntry);
                } else {
                    jarFile = new JarFile(fixedJarPath);
                    result = fetchEntityClassNames(jarFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println("Invalid URL: " + url);
        }
        return result;
    }

    private List<String> fetchEntityClassNames(JarFile jarFile) {
        List<String> result = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            collectMatchedEntryClassNames(entry, result);
        }
        return result;
    }

    private List<String> fetchNestedEntityClassNames(JarFile jarFile, JarEntry rpcEntry) {
        List<String> result = new ArrayList<>();
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(jarFile.getInputStream(rpcEntry));
            while (jis.available() == 1) {
                ZipEntry entry = jis.getNextEntry();
                collectMatchedEntryClassNames(entry, result);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            if (null != jis) {
                try {
                    jis.close();
                } catch (IOException ignored) {
                }
            }
            if (null != jarFile) {
                try {
                    jarFile.close();
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    private void collectMatchedEntryClassNames(ZipEntry entry, List<String> container) {
        String packagePrefix = BASE_PACKAGE.replace(DOT, PATH_FLAG);
        boolean matched = (
                null != entry
                        && entry.getName().startsWith(packagePrefix)
                        && entry.getName().endsWith(".class")
                        && !entry.getName().contains("$")
        );
        if (matched) {
            String className = entry.getName()
                    .substring(0, entry.getName().lastIndexOf(DOT))
                    .replace(PATH_FLAG, DOT);
            container.add(className);
        }
    }

    /**
     * 以write开头，返回为string类型，接收一个参数的所有方法
     */
    private List<Method> findSendMethods() {
        Method[] methods = Client.class.getDeclaredMethods();
        return Arrays.stream(methods)
                     .filter(m -> m.getName().startsWith(SEND_METHOD_PREFIX) && m.getReturnType() == String.class
                                  && m.getParameterCount() == 1)
                     .collect(Collectors.toList());
    }

}
