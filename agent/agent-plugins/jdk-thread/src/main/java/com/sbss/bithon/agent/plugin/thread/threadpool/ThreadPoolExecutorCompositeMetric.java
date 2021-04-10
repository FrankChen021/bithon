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

package com.sbss.bithon.agent.plugin.thread.threadpool;

import com.sbss.bithon.agent.core.metric.domain.thread.ThreadPoolCompositeMetric;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 10:48 下午
 */
class ThreadPoolExecutorCompositeMetric extends ThreadPoolCompositeMetric {

    private final ThreadPoolExecutor executor;

    ThreadPoolExecutorCompositeMetric(ThreadPoolExecutor executor) {
        super(executor.getThreadFactory().getClass().getName(),
              ThreadPoolUtils.getThreadPoolName(executor.getThreadFactory()));
        this.executor = executor;
    }

    @Override
    public long getActiveThreads() {
        return executor.getActiveCount();
    }

    @Override
    public long getCurrentPoolSize() {
        return executor.getPoolSize();
    }

    @Override
    public long getMaxPoolSize() {
        return executor.getMaximumPoolSize();
    }

    @Override
    public long getLargestPoolSize() {
        return executor.getLargestPoolSize();
    }

    @Override
    public long getQueuedTaskCount() {
        return executor.getQueue().size();
    }
}
