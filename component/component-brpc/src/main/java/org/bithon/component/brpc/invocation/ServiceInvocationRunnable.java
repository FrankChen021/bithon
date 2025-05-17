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
import java.util.concurrent.Executor;

/**
 * @author frankchen
 */
public class ServiceInvocationRunnable implements Runnable {
    private final Channel channel;

    private final long txId;
    private final ServiceRegistry.ServiceInvoker serviceInvoker;
    private final Object[] args;

    public ServiceInvocationRunnable(Channel channel,
                                     long txId,
                                     ServiceRegistry.ServiceInvoker serviceInvoker,
                                     Object[] args) {
        this.channel = channel;
        this.txId = txId;
        this.serviceInvoker = serviceInvoker;
        this.args = args;
    }


    @Override
    public void run() {
        try {
            Object ret;
            try {
                ret = serviceInvoker.invoke(this.args);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s] exception: Illegal argument",
                                              channel.remoteAddress().toString(),
                                              serviceInvoker.getMetadata().getServiceName(),
                                              serviceInvoker.getMetadata().getMethodName());
            } catch (IllegalAccessException e) {
                throw new ServiceInvocationException("[Client=%s] Service[%s#%s] exception: %s",
                                                     channel.remoteAddress().toString(),
                                                     serviceInvoker.getMetadata().getServiceName(),
                                                     serviceInvoker.getMetadata().getMethodName(),
                                                     e.getMessage());
            } catch (InvocationTargetException e) {
                throw new ServiceInvocationException(e.getTargetException(),
                                                     "[Client=%s] Service[%s#%s] invocation exception",
                                                     channel.remoteAddress().toString(),
                                                     serviceInvoker.getMetadata().getServiceName(),
                                                     serviceInvoker.getMetadata().getMethodName());
            }

            if (!serviceInvoker.getMetadata().isOneway()) {
                ServiceResponseMessageOut.builder()
                                         .serverResponseAt(System.currentTimeMillis())
                                         .txId(this.txId)
                                         .serializer(serviceInvoker.getMetadata().getSerializer())
                                         .returning(ret)
                                         .send(channel);
            }
        } catch (ServiceInvocationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            LoggerFactory.getLogger(ServiceInvocationRunnable.class).error(StringUtils.format("[Client=%s] Service Invocation on %s#%s",
                                                                                              channel.remoteAddress().toString(),
                                                                                              serviceInvoker.getMetadata().getServiceName(),
                                                                                              serviceInvoker.getMetadata().getMethodName()),
                                                                           cause);
            ServiceResponseMessageOut.builder()
                                     .serverResponseAt(System.currentTimeMillis())
                                     .txId(this.txId)
                                     .serializer(serviceInvoker.getMetadata().getSerializer())
                                     .exception(cause)
                                     .send(channel);
        }
    }

    public static void execute(ServiceRegistry serviceRegistry,
                               Channel channel,
                               ServiceRequestMessageIn serviceRequest,
                               Executor executor) {
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

            try {
                // read args outside the thread pool
                // so that the messages in the netty buffer are consumed in the netty's IO thread
                Object[] args = serviceRequest.readArgs(serviceInvoker.getParameterTypes());

                executor.execute(new ServiceInvocationRunnable(channel,
                                                               serviceRequest.getTransactionId(),
                                                               serviceInvoker,
                                                               args));
            } catch (IOException e) {
                throw new BadRequestException("[Client=%s] Bad Request: Service[%s#%s]: %s",
                                              channel.remoteAddress().toString(),
                                              serviceRequest.getServiceName(),
                                              serviceRequest.getMethodName(),
                                              e.getMessage());
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
