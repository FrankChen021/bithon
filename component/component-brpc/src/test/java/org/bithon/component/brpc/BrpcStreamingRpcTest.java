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

import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.brpc.channel.BrpcServerBuilder;
import org.bithon.component.brpc.example.IStreamingService;
import org.bithon.component.brpc.example.Person;
import org.bithon.component.brpc.example.StreamingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test class for streaming RPC functionality
 *
 * @author frankchen
 */
public class BrpcStreamingRpcTest {

    static BrpcServer brpcServer;
    static int idleSeconds = 10;
    static int serverPort;

    @BeforeEach
    public void setup() {
        serverPort = 18071;

        brpcServer = BrpcServerBuilder.builder()
                                      .serverId("streaming-test")
                                      .idleSeconds(idleSeconds)
                                      .build()
                                      .bindService(new StreamingServiceImpl())
                                      .start(serverPort);
    }

    @AfterEach
    public void teardown() {
        brpcServer.fastClose();
    }

    @Test
    public void testBasicStreaming() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {

            IStreamingService service = client.getRemoteService(IStreamingService.class);

            List<String> receivedData = new ArrayList<>();
            CountDownLatch completeLatch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    errorRef.set(throwable);
                    completeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    completeLatch.countDown();
                }
            };

            // Call streaming method
            service.streamNumbers(5, streamResponse);

            // Wait for completion
            boolean completed = completeLatch.await(3, TimeUnit.SECONDS);

            Assertions.assertTrue(completed, "Streaming should complete within 3 seconds");
            Assertions.assertNull(errorRef.get(), "No error should occur during streaming");

            // Verify received data
            synchronized (receivedData) {
                Assertions.assertEquals(5, receivedData.size(), "Should receive 5 items");
                Assertions.assertEquals(Arrays.asList("0", "1", "2", "3", "4"), receivedData);
            }
        } // Client closes fast here
    }

    @Test
    public void testStreamingWithParameters() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {

            IStreamingService service = client.getRemoteService(IStreamingService.class);

            List<String> receivedData = new ArrayList<>();
            CountDownLatch completeLatch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    errorRef.set(throwable);
                    completeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    completeLatch.countDown();
                }
            };

            // Call streaming method with parameters
            service.streamWithPrefix("test-", 3, streamResponse);

            // Wait for completion
            boolean completed = completeLatch.await(3, TimeUnit.SECONDS);
            Assertions.assertTrue(completed, "Streaming should complete within 3 seconds");
            Assertions.assertNull(errorRef.get(), "No error should occur during streaming");

            // Verify received data
            synchronized (receivedData) {
                Assertions.assertEquals(3, receivedData.size(), "Should receive 3 items");
                Assertions.assertEquals(Arrays.asList("test-0", "test-1", "test-2"), receivedData);
            }
        }
    }

    @Test
    public void testStreamingWithError() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {

            IStreamingService service = client.getRemoteService(IStreamingService.class);

            List<String> receivedData = new ArrayList<>();
            CountDownLatch errorLatch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    errorRef.set(throwable);
                    errorLatch.countDown();
                }

                @Override
                public void onComplete() {
                    // Should not be called when error occurs
                    Assertions.fail("onComplete should not be called when error occurs");
                }
            };

            // Call streaming method that throws error
            service.streamWithError(2, streamResponse);

            // Wait for error
            boolean errorOccurred = errorLatch.await(3, TimeUnit.SECONDS);
            Assertions.assertEquals(2, receivedData.size());
            Assertions.assertTrue(errorOccurred, "Error should occur within 3 seconds");
            Assertions.assertNotNull(errorRef.get(), "Error should be received");
            Assertions.assertTrue(errorRef.get()
                                          .getMessage()
                                          .contains("Simulated error"),
                                  "Error message should contain 'Simulated error'");
        }
    }

    @Test
    public void testStreamingCancellationAtClientSide_After3Items() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {

            IStreamingService service = client.getRemoteService(IStreamingService.class);

            List<String> receivedData = new ArrayList<>();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            AtomicInteger onNextCount = new AtomicInteger(0);

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                        onNextCount.incrementAndGet();
                        // Cancel after receiving 3 items
                        if (receivedData.size() >= 3) {
                            cancelled.set(true);
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    // May be called when cancelled
                }

                @Override
                public void onComplete() {
                    // May be called when cancelled
                }

                @Override
                public boolean isCancelled() {
                    return cancelled.get();
                }
            };

            // Call long-running streaming method
            service.streamNumbers(100, streamResponse);

            // Wait a bit for some data to be received
            Thread.sleep(500);

            // Verify that streaming was cancelled early
            synchronized (receivedData) {
                int receivedCount = receivedData.size();
                Assertions.assertTrue(receivedCount >= 3, "Should receive at least 3 items before cancellation");
                Assertions.assertTrue(receivedCount < 100, "Should not receive all 100 items due to cancellation");
            }
        }
    }

    @Test
    public void testStreamingWithComplexTypes() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {

            IStreamingService service = client.getRemoteService(IStreamingService.class);

            List<Person> receivedData = new ArrayList<>();
            CountDownLatch completeLatch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            StreamResponse<Person> streamResponse = new StreamResponse<Person>() {
                @Override
                public void onNext(Person data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    errorRef.set(throwable);
                    completeLatch.countDown();
                }

                @Override
                public void onComplete() {
                    completeLatch.countDown();
                }
            };

            // Call streaming method with complex types
            service.streamPersons(3, streamResponse);

            // Wait for completion
            boolean completed = completeLatch.await(3, TimeUnit.SECONDS);
            Assertions.assertTrue(completed, "Streaming should complete within 3 seconds");
            Assertions.assertNull(errorRef.get(), "No error should occur during streaming");

            // Verify received data
            synchronized (receivedData) {
                Assertions.assertEquals(3, receivedData.size(), "Should receive 3 persons");
                for (int i = 0; i < 3; i++) {
                    Person person = receivedData.get(i);
                    Assertions.assertEquals("Person" + i, person.getName());
                    Assertions.assertEquals(20 + i, person.getAge());
                }
            }
        }
    }

    @Test
    public void testMultipleConcurrentStreams() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {

            IStreamingService service = client.getRemoteService(IStreamingService.class);

            int streamCount = 3;
            CountDownLatch allCompleted = new CountDownLatch(streamCount);
            List<List<String>> allReceivedData = new ArrayList<>();

            for (int i = 0; i < streamCount; i++) {
                allReceivedData.add(new ArrayList<>());
            }

            // Start multiple concurrent streams
            for (int streamIndex = 0; streamIndex < streamCount; streamIndex++) {
                final List<String> currentStreamData = allReceivedData.get(streamIndex);

                StreamResponse<String> streamResponse = new StreamResponse<String>() {
                    @Override
                    public void onNext(String data) {
                        synchronized (currentStreamData) {
                            currentStreamData.add(data);
                        }
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        allCompleted.countDown();
                    }

                    @Override
                    public void onComplete() {
                        allCompleted.countDown();
                    }
                };

                service.streamNumbers(5, streamResponse);
            }

            // Wait for all streams to complete
            boolean allCompleted2 = allCompleted.await(5, TimeUnit.SECONDS);
            Assertions.assertTrue(allCompleted2, "All streams should complete within 5 seconds");

            // Verify each stream received correct data
            for (int i = 0; i < streamCount; i++) {
                List<String> streamData = allReceivedData.get(i);
                synchronized (streamData) {
                    Assertions.assertEquals(5, streamData.size(), "Stream " + i + " should receive 5 items");
                    Assertions.assertEquals(Arrays.asList("0", "1", "2", "3", "4"), streamData);
                }
            }
        }
    }
}
