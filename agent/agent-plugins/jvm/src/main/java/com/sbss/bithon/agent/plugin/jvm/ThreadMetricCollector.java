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

package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.metric.domain.jvm.ThreadCompositeMetric;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 8:27 下午
 */
public class ThreadMetricCollector {

    public static ThreadCompositeMetric collect() {
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        return new ThreadCompositeMetric(threadBean.getPeakThreadCount(),
                                         threadBean.getDaemonThreadCount(),
                                         threadBean.getTotalStartedThreadCount(),
                                         threadBean.getThreadCount());

    }
}
