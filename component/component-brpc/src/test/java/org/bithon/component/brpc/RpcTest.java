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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bithon.component.brpc.channel.ClientChannel;
import org.bithon.component.brpc.channel.ClientChannelBuilder;
import org.bithon.component.brpc.channel.ServerChannel;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.example.ExampleServiceImpl;
import org.bithon.component.brpc.example.IExampleService;
import org.bithon.component.brpc.example.protobuf.WebRequestMetrics;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcTest {

    static ServerChannel serverChannel;

    @BeforeClass
    public static void setup() {
        serverChannel = new ServerChannel().bindService(new ExampleServiceImpl())
                                           .start(8070);
    }

    @AfterClass
    public static void teardown() {
        System.out.println("TestCase Teardown...");
        serverChannel.close();
    }

    @Test
    public void testBasicCases() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            IServiceController serviceController = (IServiceController) exampleService;
            serviceController.debug(true);
            System.out.println("Start calling");
            Assert.assertEquals(2, exampleService.div(6, 3));
            System.out.println("End calling");

            // test primitive array
            Assert.assertArrayEquals(new int[]{1, 3, 5, 7}, exampleService.append(new int[]{1, 3, 5}, 7));

            // test primitive array
            Assert.assertArrayEquals(new String[]{"a", "b", "c"}, exampleService.append(new String[]{"a", "b"}, "c"));

            // test collection
            Assert.assertEquals(Arrays.asList("1", "3"), exampleService.delete(Arrays.asList("1", "2", "3"), 1));

            // test map
            Assert.assertEquals(
                ImmutableMap.of("k1", "v1", "k2", "v2"),
                exampleService.mergeMap(ImmutableMap.of("k1", "v1"), ImmutableMap.of("k2", "v2"))
            );
        }
    }

    @Test
    public void testNullArgument() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService service = ch.getRemoteService(IExampleService.class);

            // test the 2nd argument is null
            Assert.assertEquals(
                ImmutableMap.of("k1", "v1"),
                service.mergeMap(ImmutableMap.of("k1", "v1"), null)
            );

            // test the 1st argument is null
            Assert.assertEquals(
                ImmutableMap.of("k2", "v2"),
                service.mergeMap(null, ImmutableMap.of("k2", "v2"))
            );

            // test both arguments are null
            Assert.assertNull(service.mergeMap(null, null));
        }
    }

    @Test
    public void testSendProtobufMessage() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            Assert.assertEquals("/1", exampleService.sendWebMetrics(WebRequestMetrics.newBuilder().setUri("/1").build()));
            Assert.assertEquals("/2", exampleService.sendWebMetrics(WebRequestMetrics.newBuilder().setUri("/2").build()));
        }
    }

    @Test
    public void testMultipleSendMessageLite() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            Assert.assertEquals("/1-/2", exampleService.sendWebMetrics1(
                WebRequestMetrics.newBuilder().setUri("/1").build(),
                WebRequestMetrics.newBuilder().setUri("/2").build()
            ));

            Assert.assertEquals("/2-/3", exampleService.sendWebMetrics2(
                "/2",
                WebRequestMetrics.newBuilder().setUri("/3").build()
            ));

            Assert.assertEquals("/4-/5", exampleService.sendWebMetrics3(
                WebRequestMetrics.newBuilder().setUri("/4").build(),
                "/5"
            ));
        }
    }

    @Test
    public void testInvocationExceptionRaisedFromRemote() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            try {
                exampleService.div(6, 0);
                Assert.fail();
            } catch (ServiceInvocationException e) {
                System.out.println("Exception Occurred when calling RPC:" + e.getMessage());
                Assert.assertTrue(e.getMessage().contains("/ by zero"));
            }
        }
    }

    @Test
    public void testClientSideTimeout() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            exampleService.block(2);

            try {
                exampleService.block(6);
                Assert.fail();
            } catch (ServiceInvocationException e) {
                Assert.assertTrue(true);
            }

            ((IServiceController) exampleService).setTimeout(2000);
            try {
                exampleService.block(3);
                Assert.fail();
            } catch (ServiceInvocationException e) {
                Assert.assertTrue(true);
            }

            //wait server side to complete
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testConcurrent() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            AtomicInteger v = new AtomicInteger();
            AtomicInteger i = new AtomicInteger();
            ThreadLocalRandom.current().ints(1000, 5, 1000).parallel().forEach(divisor -> {
                try {
                    int idx = i.incrementAndGet();
                    int val = exampleService.div(divisor, 1);
                    if (val != divisor) {
                        v.incrementAndGet();
                    }
                    System.out.printf(Locale.ENGLISH, "%s:%d, ret=%s\n", Thread.currentThread().getName(), idx, val == divisor);
                } catch (ServiceInvocationException e) {
                    System.out.println(e.getMessage());
                    v.incrementAndGet();
                }
            });

            Assert.assertEquals(0, v.get());
        }
    }

    /**
     * server--call-->client
     */
    @Test
    public void testServerCallsClient() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            // bind a service at client side
            ch.bindService(new ExampleServiceImpl() {
                @Override
                public void block(int timeout) {
                    throw new RuntimeException("Not Implemented");
                }
            });

            //make sure the client has been connected to the server
            IExampleService calculator = ch.getRemoteService(IExampleService.class);

            Assert.assertEquals(20, calculator.div(100, 5));

            List<ServerChannel.Session> clients = serverChannel.getSessions();
            Assert.assertEquals(1, clients.size());

            String appId = clients.get(0).getAppId();
            IExampleService clientService = serverChannel.getRemoteService(appId, IExampleService.class);

            //
            // test service call from server to client
            //
            Assert.assertEquals(5, clientService.div(100, 20));

            //
            // test service exception thrown from client
            //
            try {
                clientService.block(2);
                Assert.fail("Should not run to here");
            } catch (ServiceInvocationException e) {
                System.out.println(e.getMessage());
                Assert.assertTrue(e.getMessage().contains("Not"));
            }

            //
            // test oneway
            //
            long start = System.currentTimeMillis();
            clientService.sendOneway("server");
            long end = System.currentTimeMillis();
            // since 'send' is a oneway method, its implementation blocking for 10 second won't affect server side running time
            Assert.assertTrue("isOneway failed", end - start < 1000);

            //wait for client execution completion
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testServerCallsMultipleDifferentClient() {
        ClientChannel ch1 = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).applicationName("client1").build();
        ClientChannel ch2 = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).applicationName("client2").build();

        // bind a service at client side
        ch1.bindService(new ExampleServiceImpl() {
            @Override
            public String ping() {
                return "pong1";
            }
        });

        ch2.bindService(new ExampleServiceImpl() {
            @Override
            public String ping() {
                return "pong2";
            }
        });

        try {
            //no clients since clients sessions have not established
            Assert.assertEquals(0, serverChannel.getSessions().size());
            Assert.assertEquals(0, serverChannel.getRemoteServices("client1", IExampleService.class).size());
            Assert.assertEquals(0, serverChannel.getRemoteServices("client2", IExampleService.class).size());

            //make sure the client has been connected to the server
            IExampleService client1 = ch1.getRemoteService(IExampleService.class);
            Assert.assertEquals(5, client1.div(100, 20));

            IExampleService client2 = ch2.getRemoteService(IExampleService.class);
            Assert.assertEquals(5, client2.div(100, 20));

            List<IExampleService> client1Services = serverChannel.getRemoteServices("client1", IExampleService.class);
            Assert.assertEquals(1, client1Services.size());
            Assert.assertEquals("pong1", client1Services.get(0).ping());

            List<IExampleService> client2Services = serverChannel.getRemoteServices("client2", IExampleService.class);
            Assert.assertEquals(1, client2Services.size());
            Assert.assertEquals("pong2", client2Services.get(0).ping());
        } finally {
            ch1.close();
            ch2.close();
        }
    }

    @Test
    public void testServerCallsMultipleSameClient() {
        ClientChannel ch1 = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).applicationName("client1").build();
        ClientChannel ch2 = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).applicationName("client1").build();

        // bind a service at client side
        ch1.bindService(new ExampleServiceImpl() {
            @Override
            public String ping() {
                return "pong1";
            }
        });

        ch2.bindService(new ExampleServiceImpl() {
            @Override
            public String ping() {
                return "pong2";
            }
        });

        try {
            //no clients since clients sessions have not established
            Assert.assertEquals(0, serverChannel.getSessions().size());
            Assert.assertEquals(0, serverChannel.getRemoteServices("client1", IExampleService.class).size());

            //make sure the client has been connected to the server
            IExampleService client1 = ch1.getRemoteService(IExampleService.class);
            Assert.assertEquals(5, client1.div(100, 20));

            IExampleService client2 = ch2.getRemoteService(IExampleService.class);
            Assert.assertEquals(5, client2.div(100, 20));
            List<IExampleService> client1Services = serverChannel.getRemoteServices("client1",
                                                                                    IExampleService.class);
            Assert.assertEquals(2, client1Services.size());
            Assert.assertEquals(ImmutableSet.of("pong1", "pong2"), ImmutableSet.of(
                client1Services.get(0).ping(),
                client1Services.get(1).ping()
            ));

            //
            // close the channel actively
            //
            ch1.close();
            Assert.assertEquals(1, serverChannel.getSessions().size());
            List<IExampleService> client2Services = serverChannel.getRemoteServices("client1",
                                                                                    IExampleService.class);
            Assert.assertEquals(1, client2Services.size());
            Assert.assertEquals("pong2", client2Services.get(0).ping());

            //
            ch2.close();
            Assert.assertEquals(0, serverChannel.getSessions().size());
            List<IExampleService> client3Services = serverChannel.getRemoteServices("client1",
                                                                                    IExampleService.class);
            Assert.assertEquals(0, client3Services.size());
        } finally {
            ch1.close();
            ch2.close();
        }
    }

    @Test
    public void testJsonSerializer() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // test map
            Assert.assertEquals(
                ImmutableMap.of("k1", "v1", "k2", "v2"),
                exampleService.mergeWithJson(
                    ImmutableMap.of("k1", "v1"),
                    ImmutableMap.of("k2", "v2"))
            );
        }
    }

    @Test
    public void testServiceWithZeroArgument() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // test map
            Assert.assertEquals("pong", exampleService.ping());

            // test IServiceControl#getPeer
            IServiceController ctrl = (IServiceController) exampleService;
            EndPoint endPoint = ctrl.getPeer();
            Assert.assertEquals("127.0.0.1", endPoint.getHost());
            Assert.assertEquals(8070, endPoint.getPort());
        }
    }

    @Test
    public void testCallNotRegisteredService() {
        try (ServerChannel serverChannel = new ServerChannel().start(18070)) {
            try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 18070).build()) {
                try {
                    // IExampleService is not registered at remote, ServiceNotFoundException should be thrown
                    ch.getRemoteService(IExampleService.class);

                    Assert.fail("Should not go to here");
                } catch (ServiceNotFoundException ignored) {
                }
            }
        }
    }

    @Test
    public void testV1Compatibility() {
        try (ClientChannel ch = ClientChannelBuilder.builder().endpointProvider("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // test map
            Assert.assertEquals("ping", exampleService.testV1Compatibility("ping"));
        }
    }
}
