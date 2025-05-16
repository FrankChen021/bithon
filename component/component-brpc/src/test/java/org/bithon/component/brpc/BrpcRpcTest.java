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
import org.bithon.component.brpc.channel.BrpcClient;
import org.bithon.component.brpc.channel.BrpcClientBuilder;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.brpc.channel.BrpcServerBuilder;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.example.ExampleServiceImpl;
import org.bithon.component.brpc.example.IExampleService;
import org.bithon.component.brpc.example.protobuf.WebRequestMetrics;
import org.bithon.component.brpc.exception.CalleeSideException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.component.brpc.message.Headers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class BrpcRpcTest {

    static BrpcServer brpcServer;
    static int idleSeconds = 5;

    @BeforeEach
    public void setup() {
        brpcServer = BrpcServerBuilder.builder()
                                      .serverId("test")
                                      .idleSeconds(idleSeconds)
                                      .build()
                                      .bindService(new ExampleServiceImpl())
                                      .start(8070);
    }

    @AfterEach
    public void teardown() {
        brpcServer.close();
    }

    @Test
    public void testBasicCases() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            IServiceController serviceController = (IServiceController) exampleService;
            serviceController.debug(true);
            System.out.println("Start calling");
            Assertions.assertEquals(2, exampleService.div(6, 3));
            System.out.println("End calling");

            // test primitive array
            Assertions.assertArrayEquals(new int[]{1, 3, 5, 7}, exampleService.append(new int[]{1, 3, 5}, 7));

            // test primitive array
            Assertions.assertArrayEquals(new String[]{"a", "b", "c"},
                                         exampleService.append(new String[]{"a", "b"}, "c"));

            // test collection
            Assertions.assertEquals(Arrays.asList("1", "3"), exampleService.delete(Arrays.asList("1", "2", "3"), 1));

            // test map
            Assertions.assertEquals(
                ImmutableMap.of("k1", "v1", "k2", "v2"),
                exampleService.mergeMap(ImmutableMap.of("k1", "v1"), ImmutableMap.of("k2", "v2"))
            );
        }
    }

    @Test
    public void testNullArgument() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService service = ch.getRemoteService(IExampleService.class);

            // test the 2nd argument is null
            Assertions.assertEquals(
                ImmutableMap.of("k1", "v1"),
                service.mergeMap(ImmutableMap.of("k1", "v1"), null)
            );

            // test the 1st argument is null
            Assertions.assertEquals(
                ImmutableMap.of("k2", "v2"),
                service.mergeMap(null, ImmutableMap.of("k2", "v2"))
            );

            // test both arguments are null
            Assertions.assertNull(service.mergeMap(null, null));
        }
    }

    @Test
    public void testSendProtobufMessage() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            Assertions.assertEquals("/1",
                                    exampleService.sendWebMetrics(WebRequestMetrics.newBuilder().setUri("/1").build()));
            Assertions.assertEquals("/2",
                                    exampleService.sendWebMetrics(WebRequestMetrics.newBuilder().setUri("/2").build()));
        }
    }

    @Test
    public void testMultipleSendMessageLite() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            Assertions.assertEquals("/1-/2", exampleService.sendWebMetrics1(
                WebRequestMetrics.newBuilder().setUri("/1").build(),
                WebRequestMetrics.newBuilder().setUri("/2").build()
            ));

            Assertions.assertEquals("/2-/3", exampleService.sendWebMetrics2(
                "/2",
                WebRequestMetrics.newBuilder().setUri("/3").build()
            ));

            Assertions.assertEquals("/4-/5", exampleService.sendWebMetrics3(
                WebRequestMetrics.newBuilder().setUri("/4").build(),
                "/5"
            ));
        }
    }

    @Test
    public void testInvocationExceptionRaisedFromRemote() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            try {
                exampleService.div(6, 0);
                Assertions.fail();
            } catch (CalleeSideException e) {
                System.out.println("Exception Occurred when calling RPC:" + e.getMessage());
                Assertions.assertEquals(ArithmeticException.class.getName(), e.getExceptionClass());
                Assertions.assertTrue(e.getMessage().contains("/ by zero"));
            }
        }
    }

    @Test
    public void testClientSideTimeout() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            exampleService.block(2);

            try {
                exampleService.block(6);
                Assertions.fail();
            } catch (ServiceInvocationException e) {
                Assertions.assertTrue(true);
            }

            ((IServiceController) exampleService).setTimeout(2000);
            try {
                exampleService.block(3);
                Assertions.fail();
            } catch (ServiceInvocationException e) {
                Assertions.assertTrue(true);
            }

            //wait server side to complete
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testConcurrency() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
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
                    System.out.printf(Locale.ENGLISH,
                                      "%s:%d, ret=%s\n",
                                      Thread.currentThread().getName(),
                                      idx,
                                      val == divisor);
                } catch (ServiceInvocationException e) {
                    System.out.println(e.getMessage());
                    v.incrementAndGet();
                }
            });

            Assertions.assertEquals(0, v.get());
        }
    }

    /**
     * server--call-->client
     */
    @Test
    public void testServerCallsClient() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            ch.setHeader(Headers.HEADER_APP_ID, "app1");

            // bind a service at client side
            ch.bindService(new ExampleServiceImpl() {
                @Override
                public void block(int timeout) {
                    throw new RuntimeException("Not Implemented");
                }
            });

            //make sure the client has been connected to the server
            IExampleService calculator = ch.getRemoteService(IExampleService.class);

            Assertions.assertEquals(20, calculator.div(100, 5));

            List<BrpcServer.Session> clients = brpcServer.getSessions();
            Assertions.assertEquals(1, clients.size());

            String appId = clients.get(0).getRemoteAttribute(Headers.HEADER_APP_ID);
            IExampleService clientService = brpcServer.getRemoteService(appId, IExampleService.class);

            //
            // test service call from server to client
            //
            Assertions.assertEquals(5, clientService.div(100, 20));

            //
            // test service exception thrown from the client
            //
            try {
                clientService.block(2);
                Assertions.fail("Should not run to here");
            } catch (ServiceInvocationException e) {
                Assertions.assertTrue(e.getMessage().contains("Not"), e.getMessage());
            }

            //
            // test oneway
            //
            long start = System.currentTimeMillis();
            clientService.sendOneway("server");
            long end = System.currentTimeMillis();
            // since 'send' is a oneway method, its implementation blocking for 10 seconds won't affect server side running time
            Assertions.assertTrue(end - start < 1000, "isOneway failed");

            //wait for client execution completion
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    @Test
    public void testServerCallsMultipleDifferentClient() {
        BrpcClient ch1 = BrpcClientBuilder.builder().server("127.0.0.1", 8070).applicationName("client1").build();
        BrpcClient ch2 = BrpcClientBuilder.builder().server("127.0.0.1", 8070).applicationName("client2").build();

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
            Assertions.assertEquals(0, brpcServer.getSessions().size());
            Assertions.assertEquals(0, brpcServer.getRemoteServices("client1", IExampleService.class).size());
            Assertions.assertEquals(0, brpcServer.getRemoteServices("client2", IExampleService.class).size());

            //make sure the client has been connected to the server
            IExampleService client1 = ch1.getRemoteService(IExampleService.class);
            Assertions.assertEquals(5, client1.div(100, 20));

            IExampleService client2 = ch2.getRemoteService(IExampleService.class);
            Assertions.assertEquals(5, client2.div(100, 20));

            List<IExampleService> client1Services = brpcServer.getRemoteServices("client1", IExampleService.class);
            Assertions.assertEquals(1, client1Services.size());
            Assertions.assertEquals("pong1", client1Services.get(0).ping());

            List<IExampleService> client2Services = brpcServer.getRemoteServices("client2", IExampleService.class);
            Assertions.assertEquals(1, client2Services.size());
            Assertions.assertEquals("pong2", client2Services.get(0).ping());
        } finally {
            ch1.close();
            ch2.close();
        }
    }

    @Test
    public void testServerCallsMultipleSameClient() {
        BrpcClient ch1 = BrpcClientBuilder.builder().server("127.0.0.1", 8070).applicationName("client1").build();
        BrpcClient ch2 = BrpcClientBuilder.builder().server("127.0.0.1", 8070).applicationName("client1").build();

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
            Assertions.assertEquals(0, brpcServer.getSessions().size());
            Assertions.assertEquals(0, brpcServer.getRemoteServices("client1", IExampleService.class).size());

            //make sure the client has been connected to the server
            IExampleService client1 = ch1.getRemoteService(IExampleService.class);
            Assertions.assertEquals(5, client1.div(100, 20));

            IExampleService client2 = ch2.getRemoteService(IExampleService.class);
            Assertions.assertEquals(5, client2.div(100, 20));
            List<IExampleService> client1Services = brpcServer.getRemoteServices("client1",
                                                                                 IExampleService.class);
            Assertions.assertEquals(2, client1Services.size());
            Assertions.assertEquals(ImmutableSet.of("pong1", "pong2"), ImmutableSet.of(
                client1Services.get(0).ping(),
                client1Services.get(1).ping()
            ));

            //
            // close the channel actively
            //
            ch1.close();
            Assertions.assertEquals(1, brpcServer.getSessions().size());
            List<IExampleService> client2Services = brpcServer.getRemoteServices("client1",
                                                                                 IExampleService.class);
            Assertions.assertEquals(1, client2Services.size());
            Assertions.assertEquals("pong2", client2Services.get(0).ping());

            //
            ch2.close();
            Assertions.assertEquals(0, brpcServer.getSessions().size());
            List<IExampleService> client3Services = brpcServer.getRemoteServices("client1",
                                                                                 IExampleService.class);
            Assertions.assertEquals(0, client3Services.size());
        } finally {
            ch1.close();
            ch2.close();
        }
    }

    @Test
    public void testJsonSerializer() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // test map
            Assertions.assertEquals(
                ImmutableMap.of("k1", "v1", "k2", "v2"),
                exampleService.mergeWithJson(
                    ImmutableMap.of("k1", "v1"),
                    ImmutableMap.of("k2", "v2"))
            );
        }
    }

    @Test
    public void testServiceWithZeroArgument() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // test map
            Assertions.assertEquals("pong", exampleService.ping());

            // test IServiceControl#getPeer
            IServiceController ctrl = (IServiceController) exampleService;
            EndPoint endPoint = ctrl.getPeer();
            Assertions.assertEquals("127.0.0.1", endPoint.getHost());
            Assertions.assertEquals(8070, endPoint.getPort());
        }
    }

    @Test
    public void testCallNotRegisteredService() {
        try (BrpcServer brpcServer = BrpcServerBuilder.builder().serverId("test").build().start(18070)) {
            try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 18070).build()) {
                try {
                    // IExampleService is not registered at remote, ServiceNotFoundException should be thrown
                    ch.getRemoteService(IExampleService.class);

                    Assertions.fail("Should not go to here");
                } catch (ServiceNotFoundException ignored) {
                }
            }
        }
    }

    @Test
    public void testV1Compatibility() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // test map
            Assertions.assertEquals("ping", exampleService.testV1Compatibility("ping"));
        }
    }

    @Test
    public void testLargeResponse() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            ((IServiceController) exampleService).setTimeout(1000_000);

            // test map
            Assertions.assertEquals(5000_000, exampleService.createList(5000_000).size());
        }
    }

    @Test
    public void testServerSideIdle() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {

            IExampleService exampleService = ch.getRemoteService(IExampleService.class);

            // Wait for server side idle
            try {
                Thread.sleep((idleSeconds + 1) * 1000L);
            } catch (InterruptedException ignored) {
            }
            // after idle timeout, the session SHOULD be cleared
            Assertions.assertEquals(0, brpcServer.getSessions().size());

            // The client side channel SHOULD be de-active
            Assertions.assertFalse(ch.isActive());

            // after the connection closed, the client will re-connect to the server once it founds that the connection is closed
            Assertions.assertEquals(5, exampleService.createList(5).size());
            Assertions.assertEquals(1, brpcServer.getSessions().size());
        }
    }

    interface INotRegisteredService {
        void empty();
    }

    @Test
    public void testServiceNotFoundException() {
        try (BrpcClient ch = BrpcClientBuilder.builder().server("127.0.0.1", 8070).build()) {
            Assertions.assertThrows(ServiceNotFoundException.class, () -> ch.getRemoteService(INotRegisteredService.class));
        }
    }

    @BrpcService(name = "not_found_test")
    interface IServiceNotFoundTest1 {
        void empty();
    }

    @BrpcService(name = "not_found_test")
    interface IServiceNotFoundTest2 {
        void empty2();
    }

    @Test
    public void testServiceNotFoundException_2() {
        // Bind service 1
        brpcServer.bindService(new IServiceNotFoundTest1() {
            @Override
            public void empty() {
            }
        });

        try (BrpcClient ch = BrpcClientBuilder.builder()
                                              .server("127.0.0.1", 8070)
                                              .build()) {

            // Use IServiceNotFoundTest2,
            // since the method name is 'empty2' does not exist in the IServiceNotFoundTest1,
            // calling on 'empty2' result in ServiceNotFoundException
            IServiceNotFoundTest2 service = ch.getRemoteService(IServiceNotFoundTest2.class);

            try {
                service.empty2();
                Assertions.fail("SHOULD NOT GO TO HERE");
            } catch (ServiceNotFoundException ignored) {
            }
        }
    }
}
