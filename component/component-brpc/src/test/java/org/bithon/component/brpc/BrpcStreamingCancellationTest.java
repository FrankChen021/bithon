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
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.commons.logging.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test class for streaming RPC cancellation functionality
 */
public class BrpcStreamingCancellationTest {

    @BrpcService
    public interface ICancellationStreamingService {
        /**
         * Stream with configurable cancellation behavior
         * 
         * @param count The number of items to stream
         * @param sleepTimeMs Time to sleep between items in milliseconds
         * @param prefix Prefix for the streamed items
         * @param response The stream response object
         */
        void streamWithCancellation(int count, int sleepTimeMs, String prefix, StreamResponse<String> response);

        /**
         * Stream with slow response to cancellation - simulates network delay
         */
        void streamWithSlowCancellationResponse(int count, StreamResponse<String> response);
    }

    // Test service implementation for cancellation testing
    public static class CancellationStreamingServiceImpl implements ICancellationStreamingService {
        @Override
        public void streamWithCancellation(int count, int sleepTimeMs, String prefix, StreamResponse<String> response) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        if (response.isCancelled()) {
                            // Actually no use
                            response.onException(new RuntimeException("Stream cancelled - prefix: " + prefix));
                            return;
                        }

                        String item = prefix + "-" + i;
                        LoggerFactory.getLogger(CancellationStreamingServiceImpl.class).info("Server sent item: " + item);
                        response.onNext(item);
                        Thread.sleep(sleepTimeMs);
                    }
                    if (!response.isCancelled()) {
                        response.onComplete();
                    }
                } catch (Exception e) {
                    response.onException(e);
                }
            }).start();
        }

        @Override
        public void streamWithSlowCancellationResponse(int count, StreamResponse<String> response) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        // Check cancellation less frequently to simulate delay
                        if (i % 3 == 0 && response.isCancelled()) {
                            response.onException(new RuntimeException("Delayed cancellation response"));
                            return;
                        }
                        response.onNext("slow-" + i);
                        Thread.sleep(100);
                    }
                    if (!response.isCancelled()) {
                        response.onComplete();
                    }
                } catch (Exception e) {
                    response.onException(e);
                }
            }).start();
        }
    }

    static BrpcServer brpcServer;
    static int idleSeconds = 10;
    static int serverPort;

    @BeforeEach
    public void setup() {
        serverPort = 18072;
        brpcServer = BrpcServerBuilder.builder()
                                      .serverId("streaming-cancellation-test")
                                      .idleSeconds(idleSeconds)
                                      .build()
                                      .bindService(new CancellationStreamingServiceImpl())
                                      .start(serverPort);
    }

    @AfterEach
    public void teardown() {
        brpcServer.fastClose();
    }

    @Test
    public void testClientSideCancellation() throws InterruptedException {
        // Fast streaming (50ms), cancel after 5 items
        int cancelAfterItems = 5;
        int sleepTimeMs = 50;
        String prefix = "fast";
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {
            ICancellationStreamingService service = client.getRemoteService(ICancellationStreamingService.class);

            List<String> receivedData = new ArrayList<>();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            CountDownLatch itemsReceivedLatch = new CountDownLatch(cancelAfterItems);

            String testName = prefix + "-" + cancelAfterItems;

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    LoggerFactory.getLogger(BrpcStreamingCancellationTest.class).info("[" + testName + "] Received data: " + data);
                    synchronized (receivedData) {
                        receivedData.add(data);
                        itemsReceivedLatch.countDown();

                        // Cancel after receiving the specified number of items
                        if (receivedData.size() == cancelAfterItems) {
                            LoggerFactory.getLogger(BrpcStreamingCancellationTest.class).info("[" + testName + "] Setting cancelled flag to true");
                            cancelled.set(true);
                            
                            this.cancel();
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    // Allow exceptions for channel closure
                    if (throwable instanceof ServiceInvocationException && throwable.getMessage().contains("Channel closed")) {
                        return;
                    }
                    Assertions.fail("[" + testName + "] onException should not be called in this case: " + throwable.toString());
                }

                @Override
                public void onComplete() {
                    // Should not be called when cancelled
                    if (cancelled.get()) {
                        Assertions.fail("[" + testName + "] onComplete should not be called when cancelled");
                    }
                }

                @Override
                public boolean isCancelled() {
                    return super.isCancelled() || cancelled.get();
                }
            };

            // Call the service with the specified parameters
            service.streamWithCancellation(20, sleepTimeMs, prefix, streamResponse);

            // Wait for the expected number of items to be received
            boolean receivedExpectedItems = itemsReceivedLatch.await(10, TimeUnit.SECONDS);

            // Verify that streaming was cancelled after receiving the expected items
            synchronized (receivedData) {
                Assertions.assertTrue(receivedExpectedItems, "[" + testName + "] Should receive expected number of items");
                Assertions.assertEquals(cancelAfterItems, receivedData.size(),
                                        "[" + testName + "] Should receive exactly " + cancelAfterItems + " items before cancellation");

                // Verify the data format matches the expected prefix
                for (int i = 0; i < cancelAfterItems; i++) {
                    Assertions.assertEquals(prefix + "-" + i, receivedData.get(i),
                                          "[" + testName + "] Item " + i + " should match expected format");
                }
            }
        }
    }

    @Test
    public void testMultipleConcurrentStreamCancellations() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {
            ICancellationStreamingService service = client.getRemoteService(ICancellationStreamingService.class);

            int streamCount = 5;
            List<List<String>> allReceivedData = new ArrayList<>();
            List<AtomicBoolean> cancellationFlags = new ArrayList<>();
            List<CountDownLatch> itemsReceivedLatches = new ArrayList<>();

            for (int i = 0; i < streamCount; i++) {
                allReceivedData.add(new ArrayList<>());
                cancellationFlags.add(new AtomicBoolean(false));
                // Create a latch for each stream with the expected item count
                itemsReceivedLatches.add(new CountDownLatch(3 + i));
            }

            // Start multiple concurrent streams
            for (int streamIndex = 0; streamIndex < streamCount; streamIndex++) {
                final List<String> currentStreamData = allReceivedData.get(streamIndex);
                final AtomicBoolean cancellationFlag = cancellationFlags.get(streamIndex);
                final CountDownLatch itemsLatch = itemsReceivedLatches.get(streamIndex);
                final int cancelAfter = 3 + streamIndex; // Cancel after different number of items
                final int finalStreamIndex = streamIndex;

                StreamResponse<String> streamResponse = new StreamResponse<String>() {
                    @Override
                    public void onNext(String data) {
                        LoggerFactory.getLogger(BrpcStreamingCancellationTest.class)
                                     .info("Stream " + finalStreamIndex + " received: " + data);
                        synchronized (currentStreamData) {
                            currentStreamData.add(data);
                            itemsLatch.countDown();

                            if (currentStreamData.size() >= cancelAfter) {
                                LoggerFactory.getLogger(BrpcStreamingCancellationTest.class)
                                             .info("Stream " + finalStreamIndex + " cancelling after " + cancelAfter + " items");
                                cancellationFlag.set(true);
                                
                                this.cancel();
                            }
                        }
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        // This won't be called after cancellation
                        LoggerFactory.getLogger(BrpcStreamingCancellationTest.class)
                                     .info("Stream " + finalStreamIndex + " exception: " + throwable.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        // Should not be called when cancelled
                        if (cancellationFlag.get()) {
                            Assertions.fail("onComplete should not be called when cancelled");
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        return super.isCancelled() || cancellationFlag.get();
                    }
                };

                service.streamWithCancellation(50, 100, "stream-" + finalStreamIndex, streamResponse);
            }

            // Wait for all streams to receive their expected items
            boolean allItemsReceived = true;
            for (int i = 0; i < streamCount; i++) {
                allItemsReceived &= itemsReceivedLatches.get(i).await(10, TimeUnit.SECONDS);
            }

            // Verify all streams received their items
            Assertions.assertTrue(allItemsReceived, "All streams should receive their expected items");

            // Verify each stream received correct number of items before cancellation
            for (int i = 0; i < streamCount; i++) {
                List<String> streamData = allReceivedData.get(i);
                int expectedItems = 3 + i;
                synchronized (streamData) {
                    Assertions.assertEquals(expectedItems, streamData.size(),
                                            "Stream " + i + " should receive " + expectedItems + " items before cancellation");
                }
            }
        }
    }

    @Test
    public void testCancellationWithDelayedResponse() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {
            ICancellationStreamingService service = client.getRemoteService(ICancellationStreamingService.class);

            List<String> receivedData = new ArrayList<>();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            CountDownLatch initialItemsLatch = new CountDownLatch(3);

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    LoggerFactory.getLogger(BrpcStreamingCancellationTest.class).info("Delayed test received: " + data);
                    synchronized (receivedData) {
                        receivedData.add(data);

                        // Count down the latch for the first 3 items
                        if (receivedData.size() <= 3) {
                            initialItemsLatch.countDown();
                        }

                        // Cancel after receiving 3 items
                        if (receivedData.size() == 3 && !cancelled.get()) {
                            LoggerFactory.getLogger(BrpcStreamingCancellationTest.class).info("Setting cancelled flag to true (delayed test)");
                            cancelled.set(true);
                            
                            this.cancel();
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    // Allow exceptions for channel closure
                    if (throwable instanceof ServiceInvocationException && throwable.getMessage().contains("Channel closed")) {
                        return;
                    }
                    Assertions.fail("onException should not be called in this case: " + throwable.toString());
                }

                @Override
                public void onComplete() {
                    // Should not be called when cancelled
                    if (cancelled.get()) {
                        Assertions.fail("onComplete should not be called when cancelled");
                    }
                }

                @Override
                public boolean isCancelled() {
                    return super.isCancelled() || cancelled.get();
                }
            };

            // Call streaming method with slow cancellation response
            service.streamWithSlowCancellationResponse(20, streamResponse);

            // Wait for initial items to be received
            boolean initialItemsReceived = initialItemsLatch.await(10, TimeUnit.SECONDS);

            // Verify that streaming was cancelled after receiving the expected items
            synchronized (receivedData) {
                Assertions.assertTrue(initialItemsReceived, "Should receive initial items");
                Assertions.assertEquals(3, receivedData.size(), "Should receive exactly 3 items before cancellation");

                // Verify the data format
                for (int i = 0; i < 3; i++) {
                    Assertions.assertEquals("slow-" + i, receivedData.get(i),
                                            "Item " + i + " should match expected format");
                }
            }
        }
    }
}
