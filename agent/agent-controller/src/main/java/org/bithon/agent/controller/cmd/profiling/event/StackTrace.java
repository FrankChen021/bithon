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

package org.bithon.agent.controller.cmd.profiling.event;


import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 13/7/25 11:33 am
 */
public class StackTrace {
    private long threadId;
    private String threadName;
    private int threadState;
    private List<StackFrame> stackFrames;

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getThreadState() {
        return threadState;
    }

    public void setThreadState(int threadState) {
        this.threadState = threadState;
    }

    public List<StackFrame> getStackFrames() {
        return stackFrames;
    }

    public void setStackFrames(List<StackFrame> stackFrames) {
        this.stackFrames = stackFrames;
    }

    @Override
    public String toString() {
        return "StackTrace{" +
               "threadId=" + threadId +
               ", threadName='" + threadName + '\'' +
               ", threadState=" + threadState + "}\n" +
               stackFrames.stream().map(StackFrame::toString).collect(Collectors.joining("\n"));
    }
}
