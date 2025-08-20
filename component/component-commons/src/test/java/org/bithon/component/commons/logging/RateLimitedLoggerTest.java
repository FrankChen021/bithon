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

package org.bithon.component.commons.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test cases for RateLimitedLogger
 *
 * @author frank.chen021@outlook.com
 * @date 2025/8/13 18:30
 */
class RateLimitedLoggerTest {

    @Mock
    private ILogAdaptor mockLogger;

    private RateLimitedLogger rateLimitedLogger;

    @BeforeEach
    void setUp() {
        this.mockLogger = Mockito.mock(ILogAdaptor.class);
        rateLimitedLogger = new RateLimitedLogger(mockLogger, Duration.ofSeconds(1));
    }

    @Test
    void testFirstLogIsAlwaysLogged() {
        // Given
        String key = "test-key";
        String message = "Test message: {}";
        String arg = "arg1";

        // When
        rateLimitedLogger.warn(key, message, arg);

        // Then - first log is always logged (suppressedCount + 1 = 1 > 0)
        verify(mockLogger, times(1)).warn(message, new Object[]{arg});
    }

    @Test
    void testSubsequentLogsWithinIntervalAreSuppressed() {
        // Given
        String key = "test-key";
        String message = "Test message";

        // When - log multiple times quickly
        rateLimitedLogger.warn(key, message);
        rateLimitedLogger.warn(key, message);
        rateLimitedLogger.warn(key, message);

        // Then - only first log should be printed (subsequent calls return 0 from getSuppressedCount)
        verify(mockLogger, times(1)).warn(message, new Object[0]);
    }

    @Test
    void testLogsAfterIntervalAreLoggedWithSuppressedCount() {
        // Given
        String key = "test-key";
        String message = "Test message";

        // When - first log
        rateLimitedLogger.warn(key, message);

        // Simulate multiple suppressed logs
        rateLimitedLogger.warn(key, message);
        rateLimitedLogger.warn(key, message);
        rateLimitedLogger.warn(key, message);

        // Wait for interval to pass (simulate time passing)
        try {
            Thread.sleep(1100); // Wait longer than 1 second interval
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Log again after interval
        rateLimitedLogger.warn(key, message);

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(mockLogger, times(2)).warn(messageCaptor.capture(), argsCaptor.capture());

        // First call should be original message
        assertEquals(message, messageCaptor.getAllValues().get(0));

        // Second call should include suppressed count (3 suppressed + 1 current = 4 total)
        String secondMessage = messageCaptor.getAllValues().get(1);
        assertTrue(secondMessage.contains("(4 logs suppressed since last logging)"));
        assertTrue(secondMessage.contains(message));
    }

    @Test
    void testSingleSuppressionShowsNoCount() {
        // Given
        String key = "test-key";
        String message = "Test message";

        // When - first log
        rateLimitedLogger.warn(key, message);

        // One suppressed log
        rateLimitedLogger.warn(key, message);

        // Wait for interval to pass
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Log again after interval
        rateLimitedLogger.warn(key, message);

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(mockLogger, times(2)).warn(messageCaptor.capture(), argsCaptor.capture());

        // Second call should NOT include count for single suppression (suppressedCount=1, condition is >1)
        String secondMessage = messageCaptor.getAllValues().get(1);
        assertFalse(secondMessage.contains("logs suppressed"));
        assertEquals(message, secondMessage);
    }

    @Test
    void testDifferentKeysAreLoggedIndependently() {
        // Given
        String key1 = "key1";
        String key2 = "key2";
        String message = "Test message";

        // When
        rateLimitedLogger.warn(key1, message);
        rateLimitedLogger.warn(key2, message);
        rateLimitedLogger.warn(key1, message); // Should be suppressed
        rateLimitedLogger.warn(key2, message); // Should be suppressed

        // Then - both first logs should be printed, subsequent ones suppressed
        verify(mockLogger, times(2)).warn(message, new Object[0]);
    }

    @Test
    void testExceptionBasedLogging() {
        // Given
        Exception exception1 = new RuntimeException("Test exception");
        Exception exception2 = new IllegalArgumentException("Another exception");
        String message = "Error occurred: {}";

        // When
        rateLimitedLogger.warn(exception1, message, exception1.getMessage());
        rateLimitedLogger.warn(exception2, message, exception2.getMessage());
        rateLimitedLogger.warn(exception1, message, exception1.getMessage()); // Should be suppressed
        rateLimitedLogger.warn(exception2, message, exception2.getMessage()); // Should be suppressed

        // Then - both first logs should be printed (different exception types)
        verify(mockLogger, times(1)).warn(eq(message), eq(new Object[]{exception1.getMessage()}));
        verify(mockLogger, times(1)).warn(eq(message), eq(new Object[]{exception2.getMessage()}));
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Given
        String key = "concurrent-key";
        String message = "Concurrent message";
        int threadCount = 10;
        int logsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalLogs = new AtomicInteger(0);

        // When - multiple threads log concurrently
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < logsPerThread; j++) {
                        rateLimitedLogger.warn(key, message);
                        totalLogs.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Then - only first log should be printed despite many concurrent attempts (due to synchronized getSuppressedCount)
        verify(mockLogger, times(1)).warn(message, new Object[0]);
        assertEquals(threadCount * logsPerThread, totalLogs.get());
    }

    @Test
    void testDifferentDurationInterval() {
        // Given - 100ms interval
        RateLimitedLogger shortIntervalLogger = new RateLimitedLogger(mockLogger, Duration.ofMillis(100));
        String key = "test-key";
        String message = "Test message";

        // When
        shortIntervalLogger.warn(key, message);
        shortIntervalLogger.warn(key, message); // Suppressed

        try {
            Thread.sleep(150); // Wait for interval to pass
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        shortIntervalLogger.warn(key, message); // Should be logged again

        // Then
        verify(mockLogger, times(2)).warn(message, new Object[0]);
    }

    @Test
    void testMessageFormatting() {
        // Given
        String key = "format-key";
        String messageFormat = "User {} performed action {} at timestamp {}";
        Object[] args = {"john", "login", 12345L};

        // When
        rateLimitedLogger.warn(key, messageFormat, args);

        // Then
        verify(mockLogger, times(1)).warn(messageFormat, args);
    }

    @Test
    void testSuppressionCountAccuracy() {
        // Given
        String key = "count-key";
        String message = "Count test message";
        int suppressedCount = 5;

        // When - first log
        rateLimitedLogger.warn(key, message);

        // Generate suppressed logs
        for (int i = 0; i < suppressedCount; i++) {
            rateLimitedLogger.warn(key, message);
        }

        try {
            Thread.sleep(1100); // Wait for interval
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Log again
        rateLimitedLogger.warn(key, message);

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(mockLogger, times(2)).warn(messageCaptor.capture(), argsCaptor.capture());

        String secondMessage = messageCaptor.getAllValues().get(1);
        // suppressedCount (5) + 1 (current) = 6 total logs attempted
        assertTrue(secondMessage.contains("(6 logs suppressed since last logging)"));
    }

    @Test
    void testResetCounterAfterLogging() {
        // Given
        String key = "reset-key";
        String message = "Reset test message";

        // When - first cycle
        rateLimitedLogger.warn(key, message);
        rateLimitedLogger.warn(key, message); // Suppressed
        rateLimitedLogger.warn(key, message); // Suppressed

        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        rateLimitedLogger.warn(key, message); // Logged with count

        // Second cycle - counter should be reset
        rateLimitedLogger.warn(key, message); // Suppressed

        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        rateLimitedLogger.warn(key, message); // Should show count of 2 (1 suppressed + 1 current), not accumulative

        // Then
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(mockLogger, times(3)).warn(messageCaptor.capture(), argsCaptor.capture());

        // Third message should show count of 2 but since it's only 1 suppressed + 1 current = 2, and condition is >1
        String thirdMessage = messageCaptor.getAllValues().get(2);
        assertFalse(thirdMessage.contains("logs suppressed")); // Only 1 suppression shows no count (condition is >1)
    }

    @Test
    void testExceptionTypeDifferentiation() {
        // Given
        RuntimeException runtimeEx = new RuntimeException();
        IllegalArgumentException illegalArgEx = new IllegalArgumentException();
        NullPointerException nullPointerEx = new NullPointerException();
        String message = "Exception message";

        // When
        rateLimitedLogger.warn(runtimeEx, message);
        rateLimitedLogger.warn(illegalArgEx, message);
        rateLimitedLogger.warn(nullPointerEx, message);

        // Same exception types should be suppressed
        rateLimitedLogger.warn(runtimeEx, message);
        rateLimitedLogger.warn(illegalArgEx, message);

        // Then - 3 different exception types should log, 2 suppressions
        verify(mockLogger, times(3)).warn(message, new Object[0]);
    }
}
