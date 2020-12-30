package com.sbss.bithon.agent.plugin.thread.threadpool;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:49 下午
 */
public class ThreadPoolUtils {

    private static final Map<String, String> THREAD_FACTORY_NAMES = new HashMap<>();

    static {
        THREAD_FACTORY_NAMES.put("java.util.concurrent.Executors$DefaultThreadFactory", "namePrefix");
        THREAD_FACTORY_NAMES.put("org.springframework.util.CustomizableThreadCreator", "threadNamePrefix");
        THREAD_FACTORY_NAMES.put("com.zaxxer.hikari.util.UtilityElf$DefaultThreadFactory", "threadName");

        THREAD_FACTORY_NAMES.put("com.alibaba.druid.util", "nameStart");
    }

    public static String getThreadPoolName(ThreadFactory threadFactory) {
        String fieldName = null;
        Class<?> factoryClass = threadFactory.getClass();
        while (factoryClass != null && fieldName == null) {
            String className = factoryClass.getName();
            fieldName = THREAD_FACTORY_NAMES.get(className);
            if (fieldName == null) {
                factoryClass = factoryClass.getSuperclass();
            }
        }
        if (fieldName != null) {
            return getThreadPoolName(threadFactory, factoryClass, new String[]{fieldName});
        } else {
            return getThreadPoolName(threadFactory, threadFactory.getClass(), THREAD_FACTORY_NAMES.values().toArray(new String[0]));
        }
    }

    public static String getThreadPoolName(ThreadFactory threadFactoryObj,
                                           Class<?> threadFactoryClass,
                                           String[] nameFields) {
        for (String nameField : nameFields) {
            try {
                Field f = threadFactoryClass.getDeclaredField(nameField);
                f.setAccessible(true);
                return stripSuffix((String) f.get(threadFactoryObj), "-");
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return String.valueOf(threadFactoryObj.hashCode());
    }

    public static String stripSuffix(String text, String suffix) {
        if (text != null) {
            if (text.endsWith("-")) {
                return text.substring(0, text.length() - 1);
            } else {
                return text;
            }
        }
        return text;
    }
}
