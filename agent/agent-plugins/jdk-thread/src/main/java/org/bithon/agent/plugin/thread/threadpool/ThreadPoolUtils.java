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

package org.bithon.agent.plugin.thread.threadpool;

import org.bithon.agent.bootstrap.expt.AgentException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
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

        THREAD_FACTORY_NAMES.put("", "name");
    }

    public static String getThreadPoolName(ThreadFactory threadFactory) {

        //
        // search the class hierachy to see if this ThreadFactory is a sub-class of above defined classes
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

    public static String getThreadPoolName(ThreadFactory threadFactoryObj,
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
        return text;
    }
}
