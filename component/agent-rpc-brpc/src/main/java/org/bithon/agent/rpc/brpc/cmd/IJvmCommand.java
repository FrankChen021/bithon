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

import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 3:50 下午
 */
@BrpcService(serializer = Serializer.JSON)
public interface IJvmCommand {

    class ThreadInfo {
        private String name;
        private long threadId;
        private boolean isDaemon;
        private int priority;
        private String state;
        private long cpuTime;
        private long userTime;
        private String stacks;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getThreadId() {
            return threadId;
        }

        public void setThreadId(long threadId) {
            this.threadId = threadId;
        }

        public boolean isDaemon() {
            return isDaemon;
        }

        public void setDaemon(boolean daemon) {
            isDaemon = daemon;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public long getCpuTime() {
            return cpuTime;
        }

        public void setCpuTime(long cpuTime) {
            this.cpuTime = cpuTime;
        }

        public long getUserTime() {
            return userTime;
        }

        public void setUserTime(long userTime) {
            this.userTime = userTime;
        }

        public String getStacks() {
            return stacks;
        }

        public void setStacks(String stacks) {
            this.stacks = stacks;
        }
    }

    List<ThreadInfo> dumpThreads();

    @Deprecated
    Collection<String> dumpClass(String pattern);

    class ClassInfo {
        private String name;
        private String classLoader;
        private boolean isSynthetic;
        private boolean isInterface;
        private boolean isAnnotation;
        private boolean isEnum;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClassLoader() {
            return classLoader;
        }

        public void setClassLoader(String classLoader) {
            this.classLoader = classLoader;
        }

        public boolean isSynthetic() {
            return isSynthetic;
        }

        public void setSynthetic(boolean synthetic) {
            isSynthetic = synthetic;
        }

        public boolean isInterface() {
            return isInterface;
        }

        public void setInterface(boolean anInterface) {
            isInterface = anInterface;
        }

        public boolean isAnnotation() {
            return isAnnotation;
        }

        public void setAnnotation(boolean annotation) {
            isAnnotation = annotation;
        }

        public boolean isEnum() {
            return isEnum;
        }

        public void setEnum(boolean anEnum) {
            isEnum = anEnum;
        }
    }

    List<ClassInfo> getLoadedClassList();
}
