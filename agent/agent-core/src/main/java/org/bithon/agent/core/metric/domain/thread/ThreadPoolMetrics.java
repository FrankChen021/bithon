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

package org.bithon.agent.core.metric.domain.thread;

import org.bithon.agent.core.metric.model.Sum;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
public abstract class ThreadPoolMetrics {

    private final String executorClass;
    private final String threadPoolName;

    public Sum callerRunTaskCount = new Sum();
    public Sum abortedTaskCount = new Sum();
    public Sum discardedTaskCount = new Sum();
    public Sum discardedOldestTaskCount = new Sum();
    public Sum exceptionTaskCount = new Sum();
    public Sum successfulTaskCount = new Sum();
    public Sum totalTaskCount = new Sum();

    public ThreadPoolMetrics(String executorClass, String threadPoolName) {
        this.executorClass = executorClass;
        this.threadPoolName = threadPoolName;
    }

    public abstract long getActiveThreads();

    public abstract long getCurrentPoolSize();

    public abstract long getMaxPoolSize();

    public abstract long getLargestPoolSize();

    public abstract long getQueuedTaskCount();

    public long getCallerRunTaskCount() {
        return callerRunTaskCount.get();
    }

    public long getAbortedTaskCount() {
        return abortedTaskCount.get();
    }

    public long getDiscardedTaskCount() {
        return discardedTaskCount.get();
    }

    public long getDiscardedOldestTaskCount() {
        return discardedOldestTaskCount.get();
    }

    public long getExceptionTaskCount() {
        return exceptionTaskCount.get();
    }

    public long getSuccessfulTaskCount() {
        return successfulTaskCount.get();
    }

    public long getTotalTaskCount() {
        return totalTaskCount.get();
    }

    public String getExecutorClass() {
        return executorClass;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }
}
