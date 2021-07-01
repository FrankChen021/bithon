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

package com.sbss.bithon.component.brpc.invocation;

import com.sbss.bithon.component.brpc.ServiceRegistry;
import com.sbss.bithon.component.brpc.exception.BadRequestException;
import com.sbss.bithon.component.brpc.exception.ServiceInvocationException;
import com.sbss.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import com.sbss.bithon.component.brpc.message.out.ServiceResponseMessageOut;
import io.netty.channel.Channel;

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
        this.channel = channel;
        this.serviceRequest = serviceRequest;
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
                throw new BadRequestException("serviceName is null");
            }

            if (serviceRequest.getMethodName() == null) {
                throw new BadRequestException("methodName is null");
            }

            ServiceRegistry.RegistryItem serviceProvider = serviceRegistry.findServiceProvider(
                serviceRequest.getMethodName());
            if (serviceProvider == null) {
                throw new BadRequestException("Can't find service provider %s#%s",
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName());
            }


            Object ret;
            try {
                Object[] inputArgs = serviceRequest.getArgs(serviceProvider.getParameterTypes());
                ret = serviceProvider.invoke(inputArgs);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Bad Request: Service[%s#%s] exception: Illegal argument",
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName());
            } catch (IllegalAccessException e) {
                throw new ServiceInvocationException("Service[%s#%s] exception: %s",
                                                     serviceRequest.getServiceName(),
                                                     serviceRequest.getMethodName(),
                                                     e.getMessage());
            } catch (InvocationTargetException e) {
                throw new ServiceInvocationException("Service[%s#%s] invocation exception: %s",
                                                     serviceRequest.getServiceName(),
                                                     serviceRequest.getMethodName(),
                                                     e.getTargetException().toString());
            } catch (IOException e) {
                throw new BadRequestException("Bad Request: Service[%s#%s]: %s",
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName(),
                                              e.getMessage());
            }

            if (!serviceProvider.isOneway()) {
                sendResponse(ServiceResponseMessageOut.builder()
                                                      .serverResponseAt(System.currentTimeMillis())
                                                      .txId(serviceRequest.getTransactionId())
                                                      .returning(ret)
                                                      .build());
            }
        } catch (ServiceInvocationException e) {
            sendResponse(ServiceResponseMessageOut.builder()
                                                  .serverResponseAt(System.currentTimeMillis())
                                                  .txId(serviceRequest.getTransactionId())
                                                  .exception(e.getMessage())
                                                  .build());
        }
    }

    private void sendResponse(ServiceResponseMessageOut serviceResponse) {
        serviceResponse.setSerializer(serviceRequest.getSerializer());
        channel.writeAndFlush(serviceResponse);
    }

}
