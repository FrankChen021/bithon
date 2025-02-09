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

package org.bithon.agent.rpc.brpc.cmd;

import org.bithon.component.brpc.BrpcService;
import org.bithon.component.brpc.message.serializer.Serializer;

import javax.management.MBeanAttributeInfo;
import java.util.List;
import java.util.Map;

/**
 * NOTE:
 * 1. the server side will automatically check if the user has permission to call methods defined in this service.
 * if the method does not start with 'get', then the WRITE permission is required.
 * <p>
 * 2. The return type of this interface should be defined as List
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 3:50 下午
 */
@BrpcService(serializer = Serializer.JSON_SMILE)
public interface IJvmCommand {

    class ThreadInfo {
        public long threadId;
        public String name;
        public boolean isDaemon;
        public int priority;
        public String state;
        public long cpuTime;
        public long userTime;
        public long blockedTime;

        /**
         * The total number of times that the thread entered the BLOCKED state
         */
        public long blockedCount;

        /**
         * The approximate accumulated elapsed time in milliseconds that a thread has been in the WAITING or TIMED_WAITING state;
         * -1 if thread contention monitoring is disabled.
         */
        public long waitedTime;

        /**
         * The total number of times that the thread was in the WAITING or TIMED_WAITING state.
         */
        public long waitedCount;

        public String lockName;
        public long lockOwnerId;
        public String lockOwnerName;
        public int inNative;
        public int suspended;
        public String stack;

        public Object[] toObjects() {
            return new Object[]{
                threadId,
                name,
                isDaemon,
                priority,
                state,
                cpuTime,
                userTime,
                blockedTime,
                blockedCount,
                waitedTime,
                waitedCount,
                lockName,
                lockOwnerId,
                lockOwnerName,
                inNative,
                suspended,
                stack
            };
        }
    }

    List<ThreadInfo> dumpThreads();

    class ClassInfo {
        public String name;
        public String classLoader;
        public boolean isSynthetic;
        public boolean isInterface;
        public boolean isAnnotation;
        public boolean isEnum;

        public Object[] toObjects() {
            return new Object[]{name, classLoader, isSynthetic, isInterface, isAnnotation, isEnum};
        }
    }

    List<ClassInfo> getLoadedClassList();

    class VMOption {
        public String name;
        public String value;
        public boolean isWriteable;
        public String origin;

        public Object[] getObjects() {
            return new Object[]{name, value, isWriteable, origin};
        }
    }

    List<VMOption> getVMOptions();

    /**
     * @param clazz the qualified class name
     */
    List<String> getAssemblyCode(String clazz);

    /**
     * get all JMX bean names
     */
    class JmxBean {
        public String domain;
        public String name;
        public String className;
        public String description;
        public Map<String, String> descriptor;

        public Object[] getObjects() {
            return new Object[]{domain, name, className, description, descriptor};
        }
    }

    List<JmxBean> getBeans();

    /**
     * get attributes of a JMX bean
     * Can be seen as a wrapper of {@link MBeanAttributeInfo}
     */
    class JmxBeanAttribute {
        public String name;
        public String type;
        public String description;
        public boolean writable;
        public boolean readable;
        public boolean is;
        public Map<String, String> descriptor;

        // The value of the attribute serialized in JSON format
        public String value;

        public Object[] getObjects() {
            return new Object[]{name, type, description, writable, readable, is, descriptor, value};
        }
    }

    List<JmxBeanAttribute> getBeanAttributes(String beanName);

    /**
     * @return value in JSON format
     */
    String getBeanAttribute(String beanName, String attributeName);
}
