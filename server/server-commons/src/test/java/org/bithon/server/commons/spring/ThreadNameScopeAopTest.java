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

package org.bithon.server.commons.spring;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class for ThreadName annotation functionality.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class ThreadNameScopeAopTest {

    @Test
    public void testStaticThreadName() {
        TestService target = new TestService();
        TestService proxiedService = createProxy(target);
        
        String originalThreadName = Thread.currentThread().getName();
        String capturedThreadName = proxiedService.methodWithStaticThreadName();
        String restoredThreadName = Thread.currentThread().getName();

        assertEquals("TestProcessor", capturedThreadName);
        assertEquals(originalThreadName, restoredThreadName);
    }

    @Test
    public void testThreadNameRestorationOnException() {
        TestService target = new TestService();
        TestService proxiedService = createProxy(target);
        
        String originalThreadName = Thread.currentThread().getName();
        
        try {
            proxiedService.methodThatThrowsException();
            fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            // Expected exception
        }
        
        String restoredThreadName = Thread.currentThread().getName();
        assertEquals(originalThreadName, restoredThreadName);
    }

    @Test
    public void testTemplateReplacement() {
        // Change current thread name to simulate a Tomcat thread
        Thread currentThread = Thread.currentThread();
        String originalName = currentThread.getName();
        currentThread.setName("http-nio-8080-exec-1");
        
        try {
            TestService target = new TestService();
            TestService proxiedService = createProxy(target);
            
            String capturedThreadName = proxiedService.methodWithTemplate();
            String restoredThreadName = Thread.currentThread().getName();

            assertEquals("WebRequest", capturedThreadName);
            assertEquals("http-nio-8080-exec-1", restoredThreadName);
        } finally {
            currentThread.setName(originalName);
        }
    }

    @Test
    public void testTemplateNoMatch() {
        // Use a thread name that doesn't match the template
        Thread currentThread = Thread.currentThread();
        String originalName = currentThread.getName();
        currentThread.setName("custom-thread-name");
        
        try {
            TestService target = new TestService();
            TestService proxiedService = createProxy(target);
            
            String capturedThreadName = proxiedService.methodWithTemplate();
            String restoredThreadName = Thread.currentThread().getName();

            // Since template doesn't match, the original thread name should remain
            // However, we want to use the value when template is provided but doesn't match
            assertEquals("WebRequest", capturedThreadName);
            assertEquals("custom-thread-name", restoredThreadName);
        } finally {
            currentThread.setName(originalName);
        }
    }

    @Test
    public void testTemplateRemoval() {
        // Test template that removes numbers
        Thread currentThread = Thread.currentThread();
        String originalName = currentThread.getName();
        currentThread.setName("thread-123-test-456");
        
        try {
            TestService target = new TestService();
            TestService proxiedService = createProxy(target);
            
            String capturedThreadName = proxiedService.methodWithRemovalTemplate();
            String restoredThreadName = Thread.currentThread().getName();

            assertEquals("thread--test-", capturedThreadName);
            assertEquals("thread-123-test-456", restoredThreadName);
        } finally {
            currentThread.setName(originalName);
        }
    }

    @Test
    public void testDetermineThreadName() {
        ThreadNameScopeAop aspect = new ThreadNameScopeAop();
        
        // Test without template (direct value usage)
        String result = callDetermineThreadName(aspect, "current-thread", "NewThreadName", "");
        assertEquals("NewThreadName", result);
        
        // Test with template that matches
        result = callDetermineThreadName(aspect, "http-nio-8080-exec-1", "WebRequest", "http-nio-\\d+-exec-\\d+");
        assertEquals("WebRequest", result);
        
        // Test with template that doesn't match
        result = callDetermineThreadName(aspect, "custom-thread", "WebRequest", "http-nio-\\d+-exec-\\d+");
        assertEquals("WebRequest", result);
        
        // Test partial replacement - replace only the matched part
        result = callDetermineThreadName(aspect, "http-nio-8080-exec-1", "MyApp-", "^([a-zA-Z-]+)");
        assertEquals("MyApp-8080-exec-1", result);
        
        // Test another partial replacement example
        result = callDetermineThreadName(aspect, "pool-1-thread-5", "worker-", "^pool-\\d+-");
        assertEquals("worker-thread-5", result);
        
        // Test replacement with capture groups
        result = callDetermineThreadName(aspect, "http-nio-8080-exec-1", "tomcat-$1-", "^http-nio-(\\d+)-");
        assertEquals("tomcat-8080-exec-1", result);
    }

    private TestService createProxy(TestService target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new ThreadNameScopeAop());
        return factory.getProxy();
    }

    private String callDetermineThreadName(ThreadNameScopeAop aspect, String currentThreadName, String value, String template) {
        // Create a mock ThreadName annotation
        ThreadNameScope mockAnnotation = new ThreadNameScope() {
            @Override
            public String value() { return value; }
            @Override
            public String template() { return template; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return ThreadNameScope.class; }
        };
        
        // Call the method directly (now package-level visibility)
        return aspect.determineThreadName(currentThreadName, mockAnnotation);
    }

    /**
     * Test service class with various @ThreadName annotated methods
     */
    public static class TestService {

        @ThreadNameScope("TestProcessor")
        public String methodWithStaticThreadName() {
            return Thread.currentThread().getName();
        }

        @ThreadNameScope("ErrorProcessor")
        public void methodThatThrowsException() {
            throw new RuntimeException("Test exception");
        }

        @ThreadNameScope(value = "WebRequest", template = "http-nio-\\d+-exec-\\d+")
        public String methodWithTemplate() {
            return Thread.currentThread().getName();
        }

        @ThreadNameScope(value = "", template = "\\d+")
        public String methodWithRemovalTemplate() {
            return Thread.currentThread().getName();
        }
    }
} 
