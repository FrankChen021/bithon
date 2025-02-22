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

import org.bithon.component.brpc.ServiceRegistry;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceResponseMessageOut;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.channel.Channel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author frankchen
 */
public class ServiceInvocationRunnable implements Runnable {
    private final ServiceRegistry serviceRegistry;
    private final Channel channel;
    private final ServiceRequestMessageIn serviceRequest;

    public ServiceInvocationRunnable(ServiceRegistry serviceRegistry,
                                     Channel channel,
                                     ServiceRequestMessageIn serviceRequest) {
        this.serviceRegistry = serviceRegistry;
        this.serviceRequest = serviceRequest;
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
            if (serviceRequest.getServiceName() == null) {
                throw new BadRequestException("[Client=%s] serviceName is null", channel.remoteAddress().toString());
            }

            if (serviceRequest.getMethodName() == null) {
                throw new BadRequestException("[Client=%s] methodName is null", channel.remoteAddress().toString());
            }

            if (!serviceRegistry.contains(serviceRequest.getServiceName())) {
                throw new ServiceNotFoundException(serviceRequest.getServiceName());
            }

            ServiceRegistry.ServiceInvoker serviceInvoker = serviceRegistry.findServiceInvoker(serviceRequest.getServiceName(),
                                                                                               serviceRequest.getMethodName());
            if (serviceInvoker == null) {
                throw new ServiceNotFoundException(serviceRequest.getServiceName() + "#" + serviceRequest.getMethodName());
            }

            Object ret;
            try {
                ret = serviceInvoker.invoke(serviceRequest.readArgs(serviceInvoker.getParameterTypes()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s] exception: Illegal argument",
                                              channel.remoteAddress().toString(),
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName());
            } catch (IllegalAccessException e) {
                throw new ServiceInvocationException("[Client=%s] Service[%s#%s] exception: %s",
                                                     channel.remoteAddress().toString(),
                                                     serviceRequest.getServiceName(),
                                                     serviceRequest.getMethodName(),
                                                     e.getMessage());
            } catch (InvocationTargetException e) {
                throw new ServiceInvocationException(e.getTargetException(),
                                                     "[Client=%s] Service[%s#%s] invocation exception",
                                                     channel.remoteAddress().toString(),
                                                     serviceRequest.getServiceName(),
                                                     serviceRequest.getMethodName());
            } catch (IOException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s]: %s",
                                              channel.remoteAddress().toString(),
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName(),
                                              e.getMessage());
            }

            if (!serviceInvoker.isOneway()) {
                ServiceResponseMessageOut.builder()
                                         .serverResponseAt(System.currentTimeMillis())
                                         .txId(serviceRequest.getTransactionId())
                                         .serializer(serviceRequest.getSerializer())
                                         .returning(ret)
                                         .send(channel);
            }
        } catch (ServiceInvocationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            boolean isClientSideException = e instanceof BadRequestException || e instanceof ServiceNotFoundException;
            if (!isClientSideException) {
                LoggerFactory.getLogger(ServiceInvocationRunnable.class).error(StringUtils.format("[Client=%s] Service Invocation on %s#%s",
                                                                                                  channel.remoteAddress().toString(),
                                                                                                  serviceRequest.getServiceName(),
                                                                                                  serviceRequest.getMethodName()),
                                                                               cause);
            }

            ServiceResponseMessageOut.builder()
                                     .serverResponseAt(System.currentTimeMillis())
                                     .txId(serviceRequest.getTransactionId())
                                     .serializer(serviceRequest.getSerializer())
                                     .exception(cause)
                                     .send(channel);
        }
    }
}
