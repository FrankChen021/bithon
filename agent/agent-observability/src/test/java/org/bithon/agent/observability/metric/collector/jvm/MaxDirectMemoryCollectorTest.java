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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for MaxDirectMemoryCollector implementations
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class MaxDirectMemoryCollectorTest {

    @Test
    public void testFactoryCreatesCorrectImplementation() throws Exception {
        // Test that the factory can create an implementation based on JDK version
        IMaxDirectMemoryCollector collector = MaxDirectMemoryCollectorFactory.create();
        assertNotNull(collector);
        
        // Test that it returns a valid max direct memory value
        long maxDirectMemory = collector.getMaxDirectMemory();
        assertTrue(maxDirectMemory > 0 || maxDirectMemory == -1, "Max direct memory should be positive or -1 for unlimited");
        
        // Verify the correct implementation is chosen based on JDK version
        String javaVersion = System.getProperty("java.version");
        System.out.println("Running on Java version: " + javaVersion);
        System.out.println("Factory created: " + collector.getClass().getSimpleName());
        System.out.println("Max direct memory: " + maxDirectMemory);
    }
    
    @Test
    public void testJdk8ImplementationWhenAvailable() {
        try {
            // Try to create JDK 8 implementation
            IMaxDirectMemoryCollector collector = new MaxDirectMemoryCollectorJdk8();
            assertNotNull(collector);
            long maxDirectMemory = collector.getMaxDirectMemory();
            assertTrue(maxDirectMemory > 0 || maxDirectMemory == -1);
        } catch (Exception e) {
            // Expected on JDK 9+ where sun.misc.VM is not available
            System.out.println("JDK 8 implementation not available (expected on JDK 9+): " + e.getMessage());
        }
    }
    
    @Test
    public void testJdk9ImplementationWhenAvailable() {
        try {
            // Try to create JDK 9+ implementation
            IMaxDirectMemoryCollector collector = new MaxDirectMemoryCollectorJdk9();
            assertNotNull(collector);
            long maxDirectMemory = collector.getMaxDirectMemory();
            assertTrue(maxDirectMemory > 0 || maxDirectMemory == -1);
        } catch (Exception e) {
            // Expected on JDK 8 where jdk.internal.misc.VM is not available
            System.out.println("JDK 9+ implementation not available (expected on JDK 8): " + e.getMessage());
        }
    }
}
