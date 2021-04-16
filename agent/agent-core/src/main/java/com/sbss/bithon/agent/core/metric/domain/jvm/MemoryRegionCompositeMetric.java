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

package com.sbss.bithon.agent.core.metric.domain.jvm;

import java.lang.management.MemoryUsage;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:54 下午
 */
public class MemoryRegionCompositeMetric {

    // approximate to -XX:MaxPermSize
    public long max = -1;

    // approximate to -XX:PermSize
    public long init = -1;

    public long used = -1;

    // available memory including used
    public long committed = -1;

    public MemoryRegionCompositeMetric() {
    }

    public MemoryRegionCompositeMetric(MemoryUsage usage) {
        this.max = usage.getMax();
        this.init = usage.getInit();
        this.used = usage.getUsed();
        this.committed = usage.getCommitted();
    }
}
