/*
 *    Copyright 2020 bithon.cn
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

package cn.bithon.rpc;

import cn.bithon.rpc.channel.ClientChannel;
import cn.bithon.rpc.channel.ServerChannel;
import cn.bithon.rpc.endpoint.EndPoint;
import cn.bithon.rpc.example.ExampleServiceImpl;
import cn.bithon.rpc.example.IExampleService;
import cn.bithon.rpc.example.WebRequestMetrics;
import cn.bithon.rpc.exception.ServiceInvocationException;
import com.google.common.collect.ImmutableMap;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcTest {

    static ServerChannel serverChannel;

    @BeforeClass
    public static void setup() {
        serverChannel = new ServerChannel()
            .bindService(IExampleService.class, new ExampleServiceImpl()).start(8070).debug(true);
    }

    @AfterClass
    public static void teardown() {
        System.out.println("TestCase Teardown...");
        serverChannel.close();
    }

    @Test
    public void test() {
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            IExampleService example = ch.getRemoteService(IExampleService.class);
            IServiceController invoker = (IServiceController) example;
            invoker.debug(true);
            System.out.println("Start calling");
            Assert.assertEquals(2, example.div(6, 3));
            System.out.println("End calling");

            // test primitive array
            Assert.assertArrayEquals(new int[]{1, 3, 5, 7}, example.append(new int[]{1, 3, 5}, 7));

            // test primitive array
            Assert.assertArrayEquals(new String[]{"a", "b", "c"}, example.append(new String[]{"a", "b"}, "c"));

            // test collection
            Assert.assertEquals(Arrays.asList("1", "3"), example.delete(Arrays.asList("1", "2", "3"), 1));

            // test map
            Assert.assertEquals(ImmutableMap.of("k1", "v1", "k2", "v2"),
                                example.merge(ImmutableMap.of("k1", "v1"), ImmutableMap.of("k2", "v2")));
        }
    }

    @Test
    public void testNull() {
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            IExampleService example = ch.getRemoteService(IExampleService.class);

            // test null
            Assert.assertEquals(ImmutableMap.of("k1", "v1"),
                                example.merge(ImmutableMap.of("k1", "v1"), null));

            // test null
            Assert.assertEquals(ImmutableMap.of("k2", "v2"),
                                example.merge(null, ImmutableMap.of("k2", "v2")));

            // test null
            Assert.assertNull(example.merge(null, null));
        }
    }

    @Test
    public void testSendMessageLite() {
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            IExampleService example = ch.getRemoteService(IExampleService.class);

            Assert.assertEquals("/1", example.send(WebRequestMetrics.newBuilder().setUri("/1").build()));
            Assert.assertEquals("/2", example.send(WebRequestMetrics.newBuilder().setUri("/2").build()));
        }
    }

    @Test
    public void testInvocationException() {
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            IExampleService calculator = ch.getRemoteService(IExampleService.class);

            try {
                calculator.div(6, 0);
                Assert.fail();
            } catch (ServiceInvocationException e) {
                System.out.println("Exception Occurred when calling RPC:" + e.getMessage());
                Assert.assertTrue(e.getMessage().contains("/ by zero"));
            }
        }
    }

    @Test
    public void testClientSideTimeout() {
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            IExampleService calculator = ch.getRemoteService(IExampleService.class);

            calculator.block(2);

            try {
                calculator.block(6);
                Assert.fail();
            } catch (ServiceInvocationException e) {
                Assert.assertTrue(true);
            }

            calculator.toController().setTimeout(2000);
            try {
                calculator.block(3);
                Assert.fail();
            } catch (ServiceInvocationException e) {
                Assert.assertTrue(true);
            }
        }
    }

    @Test
    public void testConcurrent() {
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            IExampleService calculator = ch.getRemoteService(IExampleService.class);

            AtomicInteger v = new AtomicInteger();
            AtomicInteger i = new AtomicInteger();
            ThreadLocalRandom.current().ints(1000, 5, 1000).parallel().forEach(divisor -> {
                try {
                    int idx = i.incrementAndGet();
                    int val = calculator.div(divisor, 1);
                    if (val != divisor) {
                        v.incrementAndGet();
                    }
                    System.out.printf("%s:%d, ret=%s\n", Thread.currentThread().getName(), idx, val == divisor);
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
        try (ClientChannel ch = new ClientChannel("127.0.0.1", 8070)) {
            // bind a service at client side
            ch.bindService(IExampleService.class, new ExampleServiceImpl() {
                @Override
                public void block(int timeout) {
                    throw new NotImplementedException();
                }
            });

            //make sure the client has been connected to the server
            IExampleService calculator = ch.getRemoteService(IExampleService.class);
            Assert.assertEquals(20, calculator.div(100, 5));

            Set<EndPoint> clients = serverChannel.getClientEndpoints();
            Assert.assertEquals(1, clients.size());

            EndPoint endpoint = clients.stream().findFirst().get();
            IExampleService clientCalculator = serverChannel.getRemoteService(endpoint, IExampleService.class);

            //
            // test service call from server to client
            //
            Assert.assertEquals(5, clientCalculator.div(100, 20));

            //
            // test service exception thrown from client
            //
            try {
                clientCalculator.block(2);
                Assert.fail("Should not run to here");
            } catch (ServiceInvocationException e) {
                System.out.println(e.getMessage());
                Assert.assertTrue(e.getMessage().contains("Not"));
            }

            //
            // test oneway
            //
            long start = System.currentTimeMillis();
            clientCalculator.send("server");
            long end = System.currentTimeMillis();
            // since 'send' is a oneway method, its implementation blocking for 10 second won't affect server side running time
            Assert.assertTrue(end - start < 1000);

            //wait for client execution completion
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
