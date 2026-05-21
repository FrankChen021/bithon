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

package org.bithon.component.brpc.channel;

import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelFuture;
import org.bithon.shaded.io.netty.util.concurrent.GenericFutureListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BrpcClientTest {

    @Test
    public void testPendingConnectFutureIsCancelledAfterTimeout() throws Exception {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean successful = new AtomicBoolean();
        AtomicInteger closeCount = new AtomicInteger();
        AtomicReference<Object[]> awaitArguments = new AtomicReference<>();
        AtomicReference<GenericFutureListener> lateConnectListener = new AtomicReference<>();

        Channel channel = (Channel) Proxy.newProxyInstance(
            Channel.class.getClassLoader(),
            new Class[]{Channel.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName()) && method.getParameterCount() == 0) {
                    closeCount.incrementAndGet();
                    return null;
                }
                return defaultValue(method.getReturnType());
            });

        ChannelFuture connectFuture = (ChannelFuture) Proxy.newProxyInstance(
            ChannelFuture.class.getClassLoader(),
            new Class[]{ChannelFuture.class},
            (proxy, method, args) -> {
                if ("awaitUninterruptibly".equals(method.getName())
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0] == long.class
                    && method.getParameterTypes()[1] == TimeUnit.class) {
                    awaitArguments.set(args);
                    return false;
                }

                if ("addListener".equals(method.getName())) {
                    lateConnectListener.set((GenericFutureListener) args[0]);
                    return proxy;
                }

                if ("channel".equals(method.getName())) {
                    return channel;
                }

                if ("isSuccess".equals(method.getName())) {
                    return successful.get();
                }

                if ("cancel".equals(method.getName())) {
                    cancelled.set((Boolean) args[0]);
                    return true;
                }

                return defaultValue(method.getReturnType());
            });

        Assertions.assertFalse(BrpcClient.awaitConnect(connectFuture, new EndPoint("127.0.0.1", 1), Duration.ZERO));
        Assertions.assertEquals(1L, awaitArguments.get()[0]);
        Assertions.assertEquals(TimeUnit.MILLISECONDS, awaitArguments.get()[1]);
        Assertions.assertTrue(cancelled.get());
        Assertions.assertEquals(1, closeCount.get());
        Assertions.assertNotNull(lateConnectListener.get());

        successful.set(true);
        lateConnectListener.get().operationComplete(connectFuture);
        Assertions.assertEquals(2, closeCount.get());
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
