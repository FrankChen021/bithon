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

package org.bithon.component.brpc;

import org.bithon.component.brpc.message.ServiceMessageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Unit tests for streaming method detection logic
 * 
 * @author frankchen
 */
public class BrpcStreamingMethodDetectionTest {

    @Test
    public void testStreamingMethodDetection() throws NoSuchMethodException {
        Method streamingMethod = TestService.class.getMethod("streamData", int.class, StreamResponse.class);
        ServiceRegistryItem item = ServiceRegistryItem.create(streamingMethod);
        
        Assertions.assertTrue(item.isStreaming(), "Method should be detected as streaming");
        Assertions.assertEquals(ServiceMessageType.CLIENT_STREAMING_REQUEST, item.getMessageType());
        Assertions.assertEquals(String.class, item.getStreamingDataType());
    }

    @Test
    public void testNonStreamingMethodDetection() throws NoSuchMethodException {
        Method regularMethod = TestService.class.getMethod("regularMethod", int.class);
        ServiceRegistryItem item = ServiceRegistryItem.create(regularMethod);
        
        Assertions.assertFalse(item.isStreaming(), "Method should not be detected as streaming");
        Assertions.assertNull(item.getStreamingDataType());
    }

    @Test
    public void testStreamingMethodWithComplexType() throws NoSuchMethodException {
        Method streamingMethod = TestService.class.getMethod("streamComplexData", StreamResponse.class);
        ServiceRegistryItem item = ServiceRegistryItem.create(streamingMethod);
        
        Assertions.assertTrue(item.isStreaming(), "Method should be detected as streaming");
        
        // Check if the generic type is correctly extracted
        Type streamingDataType = item.getStreamingDataType();
        Assertions.assertTrue(streamingDataType instanceof ParameterizedType, "Streaming data type should be parameterized");
        
        ParameterizedType parameterizedType = (ParameterizedType) streamingDataType;
        Assertions.assertEquals(TestData.class, parameterizedType.getRawType());
    }

    @Test
    public void testNonVoidMethodWithStreamResponse() throws NoSuchMethodException {
        Method nonVoidMethod = TestService.class.getMethod("nonVoidWithStreamResponse", StreamResponse.class);
        ServiceRegistryItem item = ServiceRegistryItem.create(nonVoidMethod);
        
        Assertions.assertFalse(item.isStreaming(), "Non-void method should not be detected as streaming");
    }

    @Test
    public void testVoidMethodWithoutStreamResponse() throws NoSuchMethodException {
        Method voidMethod = TestService.class.getMethod("voidMethodWithoutStreamResponse", String.class);
        ServiceRegistryItem item = ServiceRegistryItem.create(voidMethod);
        
        Assertions.assertFalse(item.isStreaming(), "Void method without StreamResponse should not be detected as streaming");
    }

    @Test
    public void testStreamingMethodWithMultipleParameters() throws NoSuchMethodException {
        Method streamingMethod = TestService.class.getMethod("streamWithMultipleParams", 
                                                              String.class, 
                                                              int.class, 
                                                              boolean.class, 
                                                              StreamResponse.class);
        ServiceRegistryItem item = ServiceRegistryItem.create(streamingMethod);
        
        Assertions.assertTrue(item.isStreaming(), "Method should be detected as streaming");
        Assertions.assertEquals(Double.class, item.getStreamingDataType());
    }

    // Test service interface for method detection tests
    @BrpcService
    public interface TestService {
        
        // Streaming method - void return type with StreamResponse as last parameter
        void streamData(int count, StreamResponse<String> response);
        
        // Regular method - non-void return type
        String regularMethod(int param);
        
        // Streaming method with complex generic type
        void streamComplexData(StreamResponse<TestData<Integer, String>> response);
        
        // Non-void method with StreamResponse - should NOT be streaming
        String nonVoidWithStreamResponse(StreamResponse<String> response);
        
        // Void method without StreamResponse - should NOT be streaming
        void voidMethodWithoutStreamResponse(String param);
        
        // Streaming method with multiple parameters
        void streamWithMultipleParams(String prefix, int count, boolean flag, StreamResponse<Double> response);
    }

    // Test data class for generic type testing
    public static class TestData<T, U> {
        private T first;
        private U second;
        
        public TestData() {}
        
        public TestData(T first, U second) {
            this.first = first;
            this.second = second;
        }
        
        public T getFirst() { return first; }
        public void setFirst(T first) { this.first = first; }
        
        public U getSecond() { return second; }
        public void setSecond(U second) { this.second = second; }
    }
} 
