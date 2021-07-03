/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.jvm.cmd;

import com.sbss.bithon.agent.controller.cmd.IAgentCommand;
import com.sbss.bithon.agent.rpc.brpc.cmd.IJvmCommand;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 4:38 下午
 */
public class JvmCommand implements IJvmCommand, IAgentCommand {
    @Override
    public List<ThreadInfo> dumpThreads() {

        List<ThreadInfo> threads = new ArrayList<>();

        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        boolean cpuTimeEnabled = threadMxBean.isThreadCpuTimeSupported() && threadMxBean.isThreadCpuTimeEnabled();

        Map<Thread, StackTraceElement[]> stackTraces = java.lang.Thread.getAllStackTraces();
        stackTraces.forEach((thread, stacks) -> {

            ThreadInfo threadInfo = new ThreadInfo();
            threadInfo.setName(thread.getName());
            threadInfo.setThreadId(thread.getId());
            threadInfo.setDaemon(thread.isDaemon());
            threadInfo.setPriority(thread.getPriority());
            threadInfo.setState(thread.getState().toString());
            threadInfo.setCpuTime(cpuTimeEnabled ? threadMxBean.getThreadCpuTime(thread.getId()) : -1);
            threadInfo.setUserTime(cpuTimeEnabled ? threadMxBean.getThreadUserTime(thread.getId()) : -1);
            threadInfo.setStacks(
                Arrays.stream(stacks).map(stack -> {
                    StackFrame frame = new StackFrame();
                    frame.setClassName(stack.getClassName());
                    frame.setMethodName(stack.getMethodName());
                    frame.setFileName(stack.getFileName());
                    frame.setLineNumber(stack.getLineNumber());
                    return frame;
                }).collect(Collectors.toList()));

            threads.add(threadInfo);
        });

        return threads;
    }
}
