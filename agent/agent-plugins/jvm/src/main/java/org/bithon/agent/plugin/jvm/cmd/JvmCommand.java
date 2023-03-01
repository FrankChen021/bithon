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

package org.bithon.agent.plugin.jvm.cmd;

import org.bithon.agent.controller.cmd.IAgentCommand;
import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 4:38 下午
 */
public class JvmCommand implements IJvmCommand, IAgentCommand {
    @Override
    public List<ThreadInfo> dumpThreads() {

        List<ThreadInfo> threadsInfo = new ArrayList<>();

        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        boolean cpuTimeEnabled = threadMxBean.isThreadCpuTimeSupported() && threadMxBean.isThreadCpuTimeEnabled();

        Map<Thread, StackTraceElement[]> stackTraces = java.lang.Thread.getAllStackTraces();
        stackTraces.forEach((thread, stacks) -> threadsInfo.add(toThreadInfo(threadMxBean, cpuTimeEnabled, thread, stacks)));

        return threadsInfo;
    }

    @Override
    public Collection<String> dumpClass(String pattern) {
        Pattern p = Pattern.compile(pattern);

        return Arrays.stream(InstrumentationHelper.getInstance().getAllLoadedClasses())
                     .filter(clazz -> !clazz.isSynthetic() &&
                                      !isAnonymousClassOrLambda(clazz) &&
                                      p.matcher(clazz.getName()).matches())
                     .map(Class::getName)
                     // There might be same classes loaded into different class loaders, use set to deduplicate them
                     .collect(Collectors.toSet());
    }

    @Override
    public List<ClassInfo> getLoadedClassList() {

        return Arrays.stream(InstrumentationHelper.getInstance().getAllLoadedClasses())
                     .filter(clazz -> !clazz.isSynthetic() &&
                                      // It does not make any sense to return anonymous class or lambda class
                                      !isAnonymousClassOrLambda(clazz))
                     .map((clazz) -> {
                         ClassInfo classInfo = new ClassInfo();
                         classInfo.setName(clazz.getName());
                         classInfo.setClassLoader(clazz.getClassLoader() == null ? "bootstrap" : clazz.getClassLoader().getClass().getName());
                         classInfo.setAnnotation(clazz.isAnnotation());
                         classInfo.setInterface(clazz.isInterface());
                         classInfo.setSynthetic(clazz.isSynthetic());
                         classInfo.setEnum(clazz.isEnum());

                         return classInfo;
                     })
                     // There might be same classes loaded into different class loaders, use set to deduplicate them
                     .collect(Collectors.toList());
    }

    private boolean isAnonymousClassOrLambda(Class<?> clazz) {
        try {
            return clazz.getName().indexOf('/') > 0 || clazz.isAnonymousClass();
        } catch (Throwable e) {
            // Sometime is throws IllegalAccessError internally, need to catch and ignore it
            return false;
        }
    }

    private static ThreadInfo toThreadInfo(ThreadMXBean threadMxBean, boolean cpuTimeEnabled, Thread thread, StackTraceElement[] stacks) {
        StringBuilder sb = new StringBuilder(512);
        for (StackTraceElement stack : stacks) {
            sb.append(stack.getClassName());
            sb.append('#');
            sb.append(stack.getMethodName());
            if (stack.getFileName() != null) {
                sb.append('(');
                sb.append(stack.getFileName());
                sb.append(':');
                sb.append(stack.getLineNumber());
                sb.append(')');
            }
            sb.append('\n');
        }

        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.setName(thread.getName());
        threadInfo.setThreadId(thread.getId());
        threadInfo.setDaemon(thread.isDaemon());
        threadInfo.setPriority(thread.getPriority());
        threadInfo.setState(thread.getState().toString());
        threadInfo.setCpuTime(cpuTimeEnabled ? threadMxBean.getThreadCpuTime(thread.getId()) : -1);
        threadInfo.setUserTime(cpuTimeEnabled ? threadMxBean.getThreadUserTime(thread.getId()) : -1);
        threadInfo.setStacks(sb.toString());
        return threadInfo;
    }
}
