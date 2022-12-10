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
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceResponseMessageOut;
import org.bithon.component.commons.logging.LoggerFactory;
import shaded.io.netty.channel.Channel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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

    public Channel getChannel() {
        return channel;
    }

    public ServiceRequestMessageIn getServiceRequest() {
        return serviceRequest;
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

            ServiceRegistry.ServiceInvoker serviceInvoker = serviceRegistry.findServiceInvoker(serviceRequest.getServiceName(),
                                                                                               serviceRequest.getMethodName());
            if (serviceInvoker == null) {
                throw new BadRequestException("[Client=%s] Can't find service provider %s#%s",
                                              channel.remoteAddress().toString(),
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName());
            }

            Object ret;
            try {
                ret = serviceInvoker.invoke(serviceRequest.getArgs(serviceInvoker.getParameterTypes()));
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
                throw new ServiceInvocationException("[Client=%s] Service[%s#%s] invocation exception: %s",
                                                     channel.remoteAddress().toString(),
                                                     serviceRequest.getServiceName(),
                                                     serviceRequest.getMethodName(),
                                                     e.getTargetException().toString());
            } catch (IOException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s]: %s",
                                              channel.remoteAddress().toString(),
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName(),
                                              e.getMessage());
            }

            if (!serviceInvoker.isOneway()) {
                sendResponse(ServiceResponseMessageOut.builder()
                                                      .serverResponseAt(System.currentTimeMillis())
                                                      .txId(serviceRequest.getTransactionId())
                                                      .returning(ret)
                                                      .build());
            }
        } catch (ServiceInvocationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggerFactory.getLogger(ServiceInvocationRunnable.class).warn("[Client={}] Service Invocation on {}.{} exception: {}",
                                                                          channel.remoteAddress().toString(),
                                                                          serviceRequest.getServiceName(),
                                                                          serviceRequest.getMethodName(),
                                                                          cause.toString());
            sendResponse(ServiceResponseMessageOut.builder()
                                                  .serverResponseAt(System.currentTimeMillis())
                                                  .txId(serviceRequest.getTransactionId())
                                                  .exception(cause.getMessage())
                                                  .build());
        }
    }

    private void sendResponse(ServiceResponseMessageOut serviceResponse) {
        serviceResponse.setSerializer(serviceRequest.getSerializer());
        channel.writeAndFlush(serviceResponse);
    }
}
