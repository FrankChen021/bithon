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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test class for streaming RPC channel closure handling
 *
 * @author frankchen
 */
public class BrpcStreamingChannelClosureTest {

    static BrpcServer brpcServer;
    static int idleSeconds = 5;

    @BeforeEach
    public void setup() {
        brpcServer = BrpcServerBuilder.builder()
                                      .serverId("streaming-channel-test")
                                      .idleSeconds(idleSeconds)
                                      .build()
                                      .bindService(new ChannelClosureStreamingServiceImpl())
                                      .start(8072);
    }

    @AfterEach
    public void teardown() {
        brpcServer.fastClose();
    }

    @Test
    public void testChannelClosureDuringStreaming() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.1", 8072)) {
            IChannelClosureStreamingService service = client.getRemoteService(IChannelClosureStreamingService.class);

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
                    // Should not be called when channel closes
                    Assertions.fail("onComplete should not be called when channel closes");
                }
            };

            // Start streaming
            service.streamLongRunning(100, streamResponse);

            // Let some data flow
            Thread.sleep(500);

            // Close the client (simulating unexpected channel closure)
            client.close();

            // Wait for error callback due to channel closure
            boolean errorOccurred = errorLatch.await(10, TimeUnit.SECONDS);

            // Verify that error was properly propagated
            Assertions.assertTrue(errorOccurred, "Error should occur when channel closes");
            Assertions.assertNotNull(errorRef.get(), "Error should be received");

            // Verify some data was received before closure
            synchronized (receivedData) {
                Assertions.assertTrue(!receivedData.isEmpty(), "Should receive some data before channel closure");
                Assertions.assertTrue(receivedData.size() < 100, "Should not receive all data due to channel closure");
            }

        }
    }

    @Test
    public void testServerDetectsChannelClosure() throws InterruptedException {
        try (FastShutdownBrpcClient client = new FastShutdownBrpcClient("127.0.1", 8072)) {
            IChannelClosureStreamingService service = client.getRemoteService(IChannelClosureStreamingService.class);

            CountDownLatch startLatch = new CountDownLatch(1);
            List<String> receivedData = new ArrayList<>();

            StreamResponse<String> streamResponse = new StreamResponse<String>() {
                @Override
                public void onNext(String data) {
                    synchronized (receivedData) {
                        receivedData.add(data);
                        if (receivedData.size() == 1) {
                            startLatch.countDown(); // Signal that streaming has started
                        }
                    }
                }

                @Override
                public void onException(Throwable throwable) {
                    // Expected when channel closes
                }

                @Override
                public void onComplete() {
                    // May or may not be called
                }
            };

            // Start streaming with channel monitoring
            service.streamWithChannelMonitoring(50, streamResponse);

            // Wait for streaming to start
            boolean started = startLatch.await(5, TimeUnit.SECONDS);
            Assertions.assertTrue(started, "Streaming should start");

            // Close client after streaming starts
            client.close();

            // Give server time to detect closure and stop streaming
            Thread.sleep(2000);

            // Verify that server stopped streaming due to channel closure
            synchronized (receivedData) {
                int receivedCount = receivedData.size();
                Assertions.assertTrue(receivedCount > 0, "Should receive some data");
                Assertions.assertTrue(receivedCount < 50, "Should not complete due to channel closure");
            }

        }
    }

    // Test service interface for channel closure testing
    @BrpcService
    public interface IChannelClosureStreamingService {

        /**
         * Stream data for a long time to test channel closure
         */
        void streamLongRunning(int count, StreamResponse<String> response);

        /**
         * Stream with channel monitoring to test server-side closure detection
         */
        void streamWithChannelMonitoring(int count, StreamResponse<String> response);
    }

    // Test service implementation with channel closure handling
    public static class ChannelClosureStreamingServiceImpl implements IChannelClosureStreamingService {

        @Override
        public void streamLongRunning(int count, StreamResponse<String> response) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        if (response.isCancelled()) {
                            System.out.println("Stream cancelled at item " + i);
                            break;
                        }
                        response.onNext("item-" + i);
                        Thread.sleep(100); // Delay to allow channel closure
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
        public void streamWithChannelMonitoring(int count, StreamResponse<String> response) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        // Check cancellation more frequently to detect channel closure quickly
                        if (response.isCancelled()) {
                            System.out.println("Channel monitoring detected closure at item " + i);
                            break;
                        }
                        response.onNext("monitored-" + i);
                        Thread.sleep(50); // Shorter delay for more responsive closure detection
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
} 
