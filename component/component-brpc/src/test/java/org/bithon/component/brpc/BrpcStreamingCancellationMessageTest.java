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
import org.bithon.component.brpc.message.ServiceMessageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test class for streaming RPC cancellation message transmission
 */
public class BrpcStreamingCancellationMessageTest {

    static BrpcServer brpcServer;
    static int idleSeconds = 10;
    static int serverPort;

    @BeforeEach
    public void setup() {
        serverPort = 18073;

        brpcServer = BrpcServerBuilder.builder()
                                      .serverId("streaming-cancellation-msg-test")
                                      .idleSeconds(idleSeconds)
                                      .build()
                                      .bindService(new CancellationMessageServiceImpl())
                                      .start(serverPort);
    }

    @AfterEach
    public void teardown() {
        brpcServer.fastClose();
    }

    @Test
    public void testCancellationMessageReceived() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {
            ICancellationMessageService service = client.getRemoteService(ICancellationMessageService.class);

            List<String> receivedData = new ArrayList<>();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            CountDownLatch cancellationMessageReceivedLatch = new CountDownLatch(1);

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                        // Cancel after receiving 5 items
                        if (receivedData.size() == 5) {
                            cancelled.set(true);
                            this.cancel();
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    // May be called when cancelled
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

            // Set the latch that will be counted down when the cancellation message is received
            CancellationMessageServiceImpl.cancellationMessageReceivedLatch = cancellationMessageReceivedLatch;
            
            // Call streaming method
            service.streamWithCancellationMessageTracking(50, streamResponse);

            // Wait for the cancellation message to be received
            boolean cancellationMessageReceived = cancellationMessageReceivedLatch.await(5, TimeUnit.SECONDS);
            
            // Verify that the cancellation message was received
            Assertions.assertTrue(cancellationMessageReceived, "Cancellation message should be received by the server");
            
            // Verify that streaming was cancelled after 5 items
            synchronized (receivedData) {
                Assertions.assertEquals(5, receivedData.size(), "Should receive exactly 5 items before cancellation");
            }
        }
    }

    @Test
    public void testCancellationMessageType() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {
            ICancellationMessageService service = client.getRemoteService(ICancellationMessageService.class);

            List<String> receivedData = new ArrayList<>();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            CountDownLatch cancellationMessageTypeLatch = new CountDownLatch(1);
            AtomicInteger receivedMessageType = new AtomicInteger(0);

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                        // Cancel after receiving 3 items
                        if (receivedData.size() == 3) {
                            cancelled.set(true);
                            this.cancel();
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    // May be called when cancelled
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

            // Set up the latches and references for capturing the message type
            CancellationMessageServiceImpl.cancellationMessageTypeLatch = cancellationMessageTypeLatch;
            CancellationMessageServiceImpl.receivedMessageType = receivedMessageType;
            
            // Call streaming method
            service.streamWithCancellationMessageTracking(50, streamResponse);

            // Wait for the cancellation message type to be captured
            boolean messageTypeCaptured = cancellationMessageTypeLatch.await(5, TimeUnit.SECONDS);
            
            // Verify that the message type was captured
            Assertions.assertTrue(messageTypeCaptured, "Cancellation message type should be captured");
            
            // Verify that the message type is correct
            Assertions.assertEquals(ServiceMessageType.CLIENT_STREAMING_CANCEL, receivedMessageType.get(),
                                   "Cancellation message should have the correct message type");
        }
    }

    @Test
    public void testMultipleStreamsCancellation() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.0.1", serverPort)) {
            ICancellationMessageService service = client.getRemoteService(ICancellationMessageService.class);

            int streamCount = 3;
            CountDownLatch allCancellationMessagesReceived = new CountDownLatch(streamCount);
            List<List<String>> allReceivedData = new ArrayList<>();
            List<AtomicBoolean> cancellationFlags = new ArrayList<>();

            for (int i = 0; i < streamCount; i++) {
                allReceivedData.add(new ArrayList<>());
                cancellationFlags.add(new AtomicBoolean(false));
            }

            // Set up the latch for tracking multiple cancellation messages
            CancellationMessageServiceImpl.multipleStreamsCancellationLatch = allCancellationMessagesReceived;
            
            // Start multiple concurrent streams
            for (int streamIndex = 0; streamIndex < streamCount; streamIndex++) {
                final List<String> currentStreamData = allReceivedData.get(streamIndex);
                final AtomicBoolean cancellationFlag = cancellationFlags.get(streamIndex);
                final int cancelAfter = 2 + streamIndex; // Cancel after different number of items

                StreamResponse<String> streamResponse = new StreamResponse<String>() {
                    @Override
                    public void onNext(String data) {
                        synchronized (currentStreamData) {
                            currentStreamData.add(data);
                            if (currentStreamData.size() >= cancelAfter) {
                                cancellationFlag.set(true);
                                this.cancel();
                            }
                        }
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        // Expected when cancelled
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

                service.streamWithMultipleCancellationTracking(30, streamResponse);
            }

            // Wait for all cancellation messages to be received
            boolean allMessagesReceived = allCancellationMessagesReceived.await(10, TimeUnit.SECONDS);
            
            // Verify that all cancellation messages were received
            Assertions.assertTrue(allMessagesReceived, 
                                 "All cancellation messages should be received within 10 seconds");
        }
    }

    // Test service interface for cancellation message testing
    @BrpcService
    public interface ICancellationMessageService {
        /**
         * Stream with cancellation message tracking
         */
        void streamWithCancellationMessageTracking(int count, StreamResponse<String> response);
        
        /**
         * Stream with multiple cancellation tracking
         */
        void streamWithMultipleCancellationTracking(int count, StreamResponse<String> response);
    }

    // Test service implementation for cancellation message testing
    public static class CancellationMessageServiceImpl implements ICancellationMessageService {
        // Shared latches for test coordination
        public static CountDownLatch cancellationMessageReceivedLatch;
        public static CountDownLatch cancellationMessageTypeLatch;
        public static AtomicInteger receivedMessageType;
        public static CountDownLatch multipleStreamsCancellationLatch;
        
        @Override
        public void streamWithCancellationMessageTracking(int count, StreamResponse<String> response) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        if (response.isCancelled()) {
                            // Signal that cancellation was detected
                            if (cancellationMessageReceivedLatch != null) {
                                cancellationMessageReceivedLatch.countDown();
                            }
                            
                            // Capture the message type if needed
                            if (cancellationMessageTypeLatch != null && receivedMessageType != null) {
                                receivedMessageType.set(ServiceMessageType.CLIENT_STREAMING_CANCEL);
                                cancellationMessageTypeLatch.countDown();
                            }
                            
                            // Send an exception to properly terminate the stream
                            response.onException(new RuntimeException("Stream cancelled"));
                            return;
                        }
                        response.onNext("track-" + i);
                        Thread.sleep(50);
                    }
                    if (!response.isCancelled()) {
                        response.onComplete();
                    } else {
                        // If cancelled during the last iteration, make sure we signal
                        if (cancellationMessageReceivedLatch != null) {
                            cancellationMessageReceivedLatch.countDown();
                        }
                        response.onException(new RuntimeException("Stream cancelled"));
                    }
                } catch (Exception e) {
                    response.onException(e);
                }
            }).start();
        }
        
        @Override
        public void streamWithMultipleCancellationTracking(int count, StreamResponse<String> response) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        if (response.isCancelled()) {
                            // Signal that a cancellation message was received for one of the streams
                            if (multipleStreamsCancellationLatch != null) {
                                multipleStreamsCancellationLatch.countDown();
                            }
                            // Send an exception to properly terminate the stream
                            response.onException(new RuntimeException("Stream cancelled"));
                            return;
                        }
                        response.onNext("multi-" + i);
                        Thread.sleep(50);
                    }
                    if (!response.isCancelled()) {
                        response.onComplete();
                    } else {
                        // If cancelled during the last iteration, make sure we signal
                        if (multipleStreamsCancellationLatch != null) {
                            multipleStreamsCancellationLatch.countDown();
                        }
                        response.onException(new RuntimeException("Stream cancelled"));
                    }
                } catch (Exception e) {
                    response.onException(e);
                }
            }).start();
        }
    }
}
