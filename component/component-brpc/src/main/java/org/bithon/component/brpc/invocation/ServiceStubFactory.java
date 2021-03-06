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
import org.bithon.component.brpc.channel.IChannelWriter;
import org.bithon.component.brpc.endpoint.EndPoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;

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

    @SuppressWarnings("unchecked")
    public static <T> T create(String appName, IChannelWriter channelWriter, Class<T> serviceInterface) {
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(),
                                          new Class[]{serviceInterface, IServiceController.class},
                                          new ServiceInvocationHandler(appName,
                                                                       channelWriter,
                                                                       ClientInvocationManager.getInstance()));
    }

    static class ServiceInvocationHandler implements InvocationHandler {
        private final IChannelWriter channelWriter;
        private final ClientInvocationManager clientInvocationManager;
        private final String appName;
        private boolean debugEnabled;
        private long timeout = 5000;

        public ServiceInvocationHandler(String appName,
                                        IChannelWriter channelWriter,
                                        ClientInvocationManager clientInvocationManager) {
            this.appName = appName;
            this.channelWriter = channelWriter;
            this.clientInvocationManager = clientInvocationManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (toStringMethod.equals(method)) {
                return "ServiceInvocationHandler";
            }
            if (setDebugMethod.equals(method)) {
                debugEnabled = (boolean) args[0];
                return null;
            }
            if (setTimeoutMethod.equals(method)) {
                this.timeout = (long) args[0];
                return null;
            }
            if (rstTimeoutMethod.equals(method)) {
                this.timeout = 5000;
                return null;
            }
            if (getPeerMethod.equals(method)) {
                InetSocketAddress socketAddress = (InetSocketAddress) channelWriter.getChannel().remoteAddress();
                return EndPoint.of(socketAddress);
            }
            if (getChannelMethod.equals(method)) {
                return this.channelWriter;
            }
            return clientInvocationManager.invoke(appName, channelWriter, debugEnabled, timeout, method, args);
        }
    }
}
