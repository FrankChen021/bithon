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

package org.bithon.agent.plugin.thread.utils;

import org.bithon.agent.bootstrap.expt.AgentException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:49 下午
 */
public class ThreadPoolUtils {

    /**
     * Since defaultThreadFactory returns a new object for each call,
     * in order to reduce unnecessary object creation, we cache its class name
     */
    private static final String DEFAULT_THREAD_FACTORY = Executors.defaultThreadFactory().getClass().getName();

    private static final Map<String, String> THREAD_FACTORY_NAMES = new HashMap<>();

    // TODO: move to configuration
    static {
        THREAD_FACTORY_NAMES.put("java.util.concurrent.Executors$DefaultThreadFactory", "namePrefix");
        THREAD_FACTORY_NAMES.put("org.springframework.util.CustomizableThreadCreator", "threadNamePrefix");
        THREAD_FACTORY_NAMES.put("com.zaxxer.hikari.util.UtilityElf$DefaultThreadFactory", "threadName");
        THREAD_FACTORY_NAMES.put("com.alibaba.druid.util", "nameStart");
        THREAD_FACTORY_NAMES.put("1", "name");
        THREAD_FACTORY_NAMES.put("2", "nameFormat");

        // com.google.common.util.concurrent.ThreadFactoryBuilder creates an anonymous thread factory,
        // and java compiles the nameFormat as val$nameFormat
        THREAD_FACTORY_NAMES.put("3", "val$nameFormat");
    }

    public static String detectThreadPoolName(ThreadPoolExecutor executor) {
        //
        // For default thread factory, it's pool name is meaningless
        // So, we find the caller name as the thread pool name,
        //
        ThreadFactory threadFactory = executor.getThreadFactory();
        if (DEFAULT_THREAD_FACTORY.equals(threadFactory.getClass().getName())) {
            StackTraceElement[] stackTraceElements = new RuntimeException().getStackTrace();

            // index 0 is current method, skip it
            for (int i = 1; i < stackTraceElements.length; i++) {
                StackTraceElement stack = stackTraceElements[i];
                if (!stack.getClassName().startsWith("java.util.concurrent")
                    && !stack.getClassName().startsWith("org.bithon.agent.plugin.thread")) {
                    return stack.getClassName();
                }
            }
        }

        return detectThreadPoolNameFromField(threadFactory);
    }

    private static String detectThreadPoolNameFromField(ThreadFactory threadFactory) {
        //
        // search the class hierarchy to see if this ThreadFactory is a subclass of above defined classes
        //
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
            return getThreadPoolName(threadFactory, factoryClass, Collections.singletonList(fieldName));
        } else {
            return getThreadPoolName(threadFactory,
                                     threadFactory.getClass(),
                                     THREAD_FACTORY_NAMES.values());
        }
    }

    private static String getThreadPoolName(ThreadFactory threadFactoryObj,
                                            Class<?> threadFactoryClass,
                                            Collection<String> nameFields) {
        for (String nameField : nameFields) {
            try {
                Field f = threadFactoryClass.getDeclaredField(nameField);
                f.setAccessible(true);
                return stripSuffix((String) f.get(threadFactoryObj), "-");
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }

        // ignore those ThreadFactory(s) which don't define a name field
        throw new AgentException("No name field defined on ThreadFactory class [%s]", threadFactoryClass.getName());
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
