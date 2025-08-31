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

package org.bithon.agent.observability.metric.collector.jvm;

import jdk.internal.misc.VM;

/**
 * JDK 9+ implementation for collecting maximum direct memory using jdk.internal.misc.VM.
 * This implementation directly accesses the VM class without reflection to avoid
 * module access issues. This class will only be instantiated on JDK 9+ environments
 * where jdk.internal.misc.VM is available.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/08/30
 */
class MaxDirectMemoryCollectorJdk9 implements DirectMemoryCollector.IMaxDirectMemoryGetter {

    @Override
    public long getMaxDirectMemory() {
        return VM.maxDirectMemory();
    }
}


