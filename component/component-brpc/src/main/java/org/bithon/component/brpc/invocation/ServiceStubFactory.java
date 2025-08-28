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

package org.bithon.component.brpc.invocation;

import org.bithon.component.brpc.IServiceController;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.commons.utils.Preconditions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author frankchen
 */
public class ServiceStubFactory {

    private static Method setDebugMethod;
    private static Method toStringMethod;
    private static Method setTimeoutMethod;
    private static Method rstTimeoutMethod;
    private static Method getPeerMethod;
    private static Method getChannelMethod;

    static {
        try {
            toStringMethod = Object.class.getMethod("toString");
            setDebugMethod = IServiceController.class.getMethod("debug", boolean.class);
            setTimeoutMethod = IServiceController.class.getMethod("setTimeout", long.class);
            rstTimeoutMethod = IServiceController.class.getMethod("rstTimeout");
            getPeerMethod = IServiceController.class.getMethod("getPeer");
            getChannelMethod = IServiceController.class.getMethod("getChannel");
        } catch (NoSuchMethodException ignored) {
        }
    }

    public static <T> T create(String clientAppName,
                               Headers headers,
                               IBrpcChannel channel,
                               Class<T> serviceInterface,
                               InvocationManager invocationManager) {
        return create(clientAppName, headers, channel, serviceInterface, 5000, invocationManager);
    }

    /**
     *
     * @param timeout in milliseconds
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(String clientAppName,
                               Headers headers,
                               IBrpcChannel channel,
                               Class<T> serviceInterface,
                               int timeout,
                               InvocationManager invocationManager) {
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                                          new Class[]{serviceInterface, IServiceController.class},
                                          new ServiceInvocationStub(clientAppName,
                                                                    headers,
                                                                    channel,
                                                                    invocationManager,
                                                                    timeout));
    }

    /**
     * Service stub that proxies a remote service
     */
    static class ServiceInvocationStub implements InvocationHandler {
        private final IBrpcChannel channel;
        private final InvocationManager invocationManager;
        private final String appName;
        private final Headers headers;
        private long timeout;
        private final long defaultTimeout;

        public ServiceInvocationStub(String appName,
                                     Headers headers,
                                     IBrpcChannel channel,
                                     InvocationManager invocationManager,
                                     int timeout) {
            Preconditions.checkIfTrue(timeout > 0, "timeout must be greater than zero.");

            this.appName = appName;
            this.headers = headers;
            this.channel = channel;
            this.invocationManager = invocationManager;
            this.timeout = timeout;
            this.defaultTimeout = timeout;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (toStringMethod.equals(method)) {
                return "ServiceInvocationHandler";
            }
            if (setDebugMethod.equals(method)) {
                return null;
            }
            if (setTimeoutMethod.equals(method)) {
                this.timeout = (long) args[0];
                return null;
            }
            if (rstTimeoutMethod.equals(method)) {
                this.timeout = defaultTimeout;
                return null;
            }
            if (getPeerMethod.equals(method)) {
                return channel.getRemoteAddress();
            }
            if (getChannelMethod.equals(method)) {
                return this.channel;
            }
            return invocationManager.invoke(appName,
                                            headers,
                                            channel,
                                            timeout,
                                            method,
                                            args);
        }
    }
}
