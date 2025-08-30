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

import java.lang.reflect.Method;

/**
 * JDK 8 implementation for collecting maximum direct memory using sun.misc.VM.
 * This implementation uses reflection to access the VM class so it can compile
 * on any JDK version but still work correctly on JDK 8.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
class MaxDirectMemoryCollectorJdk8 implements IMaxDirectMemoryCollector {

    private final Method maxDirectMemoryMethod;

    MaxDirectMemoryCollectorJdk8() throws Exception {
        try {
            Class<?> vmClass = Class.forName("sun.misc.VM");
            maxDirectMemoryMethod = vmClass.getDeclaredMethod("maxDirectMemory");
            maxDirectMemoryMethod.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new Exception("sun.misc.VM.maxDirectMemory() not available", e);
        }
    }

    @Override
    public long getMaxDirectMemory() {
        try {
            return (Long) maxDirectMemoryMethod.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke sun.misc.VM.maxDirectMemory()", e);
        }
    }
}
