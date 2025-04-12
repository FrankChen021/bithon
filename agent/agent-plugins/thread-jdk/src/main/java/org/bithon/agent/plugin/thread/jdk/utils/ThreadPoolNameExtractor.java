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

package org.bithon.agent.plugin.thread.jdk.utils;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.component.commons.utils.ReflectionUtils;

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
public class ThreadPoolNameExtractor {

    public static final ThreadPoolNameExtractor INSTANCE = new ThreadPoolNameExtractor();

    /**
     * Since defaultThreadFactory returns a new object for each call,
     * we cache its class name to reduce unnecessary object creation
     */
    private static final String DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory().getClass().getName();
    static final String[] SUFFIX_LIST = {"-", ".", "/", "%d", "%n", "%s"};

    /**
     * key - class name of a thread factory
     * val - field name in the thread factory that holds the name format of thread
     */
    private final Map<String, String> nameFormatFields = new ConcurrentHashMap<>();

    /**
     * TODO: move to configuration
     */
    ThreadPoolNameExtractor() {
        nameFormatFields.put("java.util.concurrent.Executors$DefaultThreadFactory", "namePrefix");
        nameFormatFields.put("org.springframework.util.CustomizableThreadCreator", "threadNamePrefix");
        nameFormatFields.put("com.zaxxer.hikari.util.UtilityElf$DefaultThreadFactory", "threadName");
        nameFormatFields.put("com.alibaba.druid.util", "nameStart");

        // Special cases to search name on a list of formats
        nameFormatFields.put("1", "name");
        nameFormatFields.put("2", "nameFormat");

        // com.google.common.util.concurrent.ThreadFactoryBuilder creates an anonymous thread factory,
        // and java compiles the nameFormat as val$nameFormat
        nameFormatFields.put("3", "val$nameFormat");
    }

    public String extract(ThreadPoolExecutor executor) {
        String name;
        //
        // For the default thread factory, its pool name is meaningless,
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
            fieldName = nameFormatFields.get(className);
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
                                        // Use a hash set to deduplicate values
                                        new HashSet<>(nameFormatFields.values()));
        }
    }

    private String getNameOnGivenFields(ThreadFactory threadFactory,
                                        Class<?> threadFactoryClass,
                                        Collection<String> nameFields) {
        for (String nameField : nameFields) {
            String name = (String) ReflectionUtils.getFieldValue(threadFactory, nameField);
            name = stripSuffix(name, SUFFIX_LIST);
            if (name != null) {
                // Save the class and the name of field to save further time
                nameFormatFields.putIfAbsent(threadFactoryClass.getName(), nameField);

                return name;
            }
        }
        return null;
    }

    public static String stripSuffix(String text, String... suffixList) {
        if (text == null) {
            return null;
        }
        boolean running;
        do {
            running = false;

            text = text.trim();
            for (String suffix : suffixList) {
                if (text.endsWith(suffix)) {
                    text = text.substring(0, text.length() - suffix.length());

                    // Continue to remove suffixes
                    running = !text.isEmpty();
                    break;
                }
            }
        } while (running);

        return text;
    }
}
