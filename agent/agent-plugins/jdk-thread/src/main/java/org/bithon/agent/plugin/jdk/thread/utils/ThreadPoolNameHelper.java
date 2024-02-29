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

package org.bithon.agent.plugin.jdk.thread.utils;

import org.bithon.agent.instrumentation.expt.AgentException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:49 下午
 */
public class ThreadPoolNameHelper {

    public static final ThreadPoolNameHelper INSTANCE = new ThreadPoolNameHelper();

    /**
     * Since defaultThreadFactory returns a new object for each call,
     * we cache its class name to reduce unnecessary object creation
     */
    private static final String DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory().getClass().getName();

    /**
     * key - class name of a thread factory
     * val - field name in the thread factory that holds the name format of thread
     */
    private final Map<String, String> threadFactoryNames = new ConcurrentHashMap<>();

    /**
     * TODO: move to configuration
     */
    ThreadPoolNameHelper() {
        threadFactoryNames.put("java.util.concurrent.Executors$DefaultThreadFactory", "namePrefix");
        threadFactoryNames.put("org.springframework.util.CustomizableThreadCreator", "threadNamePrefix");
        threadFactoryNames.put("com.zaxxer.hikari.util.UtilityElf$DefaultThreadFactory", "threadName");
        threadFactoryNames.put("com.alibaba.druid.util", "nameStart");

        // Special cases to search name on a list of formats
        threadFactoryNames.put("1", "name");
        threadFactoryNames.put("2", "nameFormat");

        // com.google.common.util.concurrent.ThreadFactoryBuilder creates an anonymous thread factory,
        // and java compiles the nameFormat as val$nameFormat
        threadFactoryNames.put("3", "val$nameFormat");
    }

    public String getThreadPoolName(ThreadPoolExecutor executor) {
        String name;
        //
        // For the default thread factory, it's pool name is meaningless,
        // So, we find the caller name as the thread pool name,
        //
        ThreadFactory threadFactory = executor.getThreadFactory();
        if (DEFAULT_THREAD_FACTORY.equals(threadFactory.getClass().getName())) {
            name = getCallerName();
            if (name != null) {
                return name;
            }
        }

        name = tryGetNameOnField(threadFactory);
        if (name == null) {
            name = getCallerName();
        }
        if (name == null) {
            // ignore those ThreadFactory(s) which don't define a name field
            throw new AgentException("No name field defined on ThreadFactory class [%s]", executor.getClass().getName());
        }
        return name;
    }

    private static String getCallerName() {
        StackTraceElement[] stackTraceElements = new RuntimeException().getStackTrace();

        // index 0 is the current method, skip it
        for (int i = 1; i < stackTraceElements.length; i++) {
            StackTraceElement stack = stackTraceElements[i];

            // Skip the method call from the JDK/bithon to find the user's method
            String className = stack.getClassName();
            if (className.startsWith("java.util.concurrent")
                || className.startsWith("org.bithon.agent.plugin.jdk.thread")) {
                continue;
            }

            return className + "#L" + stack.getLineNumber();
        }

        // Should not go here
        return null;
    }

    private String tryGetNameOnField(ThreadFactory threadFactory) {
        //
        // Search the class hierarchy to see if this ThreadFactory is a subclass of above-defined classes
        //
        String fieldName = null;
        Class<?> factoryClass = threadFactory.getClass();
        while (factoryClass != null && fieldName == null) {
            String className = factoryClass.getName();
            fieldName = threadFactoryNames.get(className);
            if (fieldName == null) {
                // Current ThreadFactory does not exist in the map, search for its parent
                factoryClass = factoryClass.getSuperclass();
            }
        }

        if (fieldName != null) {
            return getNameOnGivenFields(threadFactory, factoryClass, Collections.singletonList(fieldName));
        } else {
            // Try to get the name on known fields
            return getNameOnGivenFields(threadFactory,
                                        threadFactory.getClass(),
                                        // Use a hash set to deduplicate
                                        new HashSet<>(threadFactoryNames.values()));
        }
    }

    private String getNameOnGivenFields(ThreadFactory threadFactoryObj,
                                        Class<?> threadFactoryClass,
                                        Collection<String> nameFields) {
        for (String nameField : nameFields) {
            try {
                Field field = threadFactoryClass.getDeclaredField(nameField);
                field.setAccessible(true);
                String name = stripSuffix((String) field.get(threadFactoryObj), "-");
                if (name != null) {
                    // Save the class and the name of field to save further time
                    threadFactoryNames.putIfAbsent(threadFactoryClass.getName(), nameField);
                }
                return name;
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static String stripSuffix(String text, String suffix) {
        if (text != null) {
            if (text.endsWith(suffix)) {
                return text.substring(0, text.length() - suffix.length());
            } else {
                return text;
            }
        }
        return null;
    }
}
