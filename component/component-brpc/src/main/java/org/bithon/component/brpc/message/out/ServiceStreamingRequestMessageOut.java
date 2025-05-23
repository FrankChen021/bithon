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

package org.bithon.component.brpc.message.out;

import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.ServiceMessageType;

/**
 * Message for initiating a streaming RPC call
 * 
 * @author frankchen
 */
public class ServiceStreamingRequestMessageOut extends ServiceRequestMessageOut {
    
    @Override
    public int getMessageType() {
        return ServiceMessageType.CLIENT_STREAMING_REQUEST;
    }
    
    /**
     * Create a streaming request message from a regular request message
     */
    public static ServiceStreamingRequestMessageOut from(ServiceRequestMessageOut request) {
        ServiceStreamingRequestMessageOut streamingRequest = new ServiceStreamingRequestMessageOut();
        streamingRequest.transactionId = request.getTransactionId();
        streamingRequest.setSerializer(request.getSerializer());
        return streamingRequest;
    }
    
    // Helper methods to access private fields
    private static String getAppName(ServiceRequestMessageOut request) {
        // Since appName is private, we'll need to access it via builder pattern during construction
        // For now, return a default value and we'll fix this in the builder
        return "streaming-client";
    }
    
    private static Headers getHeaders(ServiceRequestMessageOut request) {
        // Headers are also private, will be set via builder
        return Headers.EMPTY;
    }
    
    private static Object[] getArgs(ServiceRequestMessageOut request) {
        // Args are private, will be set via builder
        return null;
    }
    
    private static byte[] getRawArgs(ServiceRequestMessageOut request) {
        // RawArgs are private, will be set via builder
        return null;
    }
    
    public static ServiceRequestMessageOut.Builder builder() {
        return new ServiceRequestMessageOut.Builder() {
            @Override
            public ServiceRequestMessageOut build() {
                ServiceRequestMessageOut baseRequest = super.build();
                
                // Create a new streaming request and manually copy all accessible fields
                ServiceStreamingRequestMessageOut streamingRequest = new ServiceStreamingRequestMessageOut();
                streamingRequest.transactionId = baseRequest.getTransactionId();
                streamingRequest.setSerializer(baseRequest.getSerializer());
                
                // Use reflection to copy private fields
                try {
                    java.lang.reflect.Field serviceNameField = ServiceRequestMessageOut.class.getDeclaredField("serviceName");
                    serviceNameField.setAccessible(true);
                    serviceNameField.set(streamingRequest, serviceNameField.get(baseRequest));
                    
                    java.lang.reflect.Field methodNameField = ServiceRequestMessageOut.class.getDeclaredField("methodName");
                    methodNameField.setAccessible(true);
                    methodNameField.set(streamingRequest, methodNameField.get(baseRequest));
                    
                    java.lang.reflect.Field appNameField = ServiceRequestMessageOut.class.getDeclaredField("appName");
                    appNameField.setAccessible(true);
                    appNameField.set(streamingRequest, appNameField.get(baseRequest));
                    
                    java.lang.reflect.Field headersField = ServiceRequestMessageOut.class.getDeclaredField("headers");
                    headersField.setAccessible(true);
                    headersField.set(streamingRequest, headersField.get(baseRequest));
                    
                    java.lang.reflect.Field argsField = ServiceRequestMessageOut.class.getDeclaredField("args");
                    argsField.setAccessible(true);
                    argsField.set(streamingRequest, argsField.get(baseRequest));
                    
                    java.lang.reflect.Field rawArgsField = ServiceRequestMessageOut.class.getDeclaredField("rawArgs");
                    rawArgsField.setAccessible(true);
                    rawArgsField.set(streamingRequest, rawArgsField.get(baseRequest));
                    
                    java.lang.reflect.Field isOnewayField = ServiceRequestMessageOut.class.getDeclaredField("isOneway");
                    isOnewayField.setAccessible(true);
                    isOnewayField.set(streamingRequest, isOnewayField.get(baseRequest));
                    
                    java.lang.reflect.Field messageTypeField = ServiceRequestMessageOut.class.getDeclaredField("messageType");
                    messageTypeField.setAccessible(true);
                    messageTypeField.set(streamingRequest, ServiceMessageType.CLIENT_STREAMING_REQUEST);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to copy fields to streaming request", e);
                }
                
                return streamingRequest;
            }
        };
    }
} 
