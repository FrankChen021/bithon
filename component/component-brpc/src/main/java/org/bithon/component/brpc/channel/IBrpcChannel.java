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
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;

import java.io.IOException;

/**
 * @author frankchen
 */
public interface IBrpcChannel {

    default void connect() {}

    default void disconnect() {}

    /**
     * How long this connection has been set up in millisecond
     */
    long getConnectionLifeTime();

    /**
     * Check if the underlying channel is active
     */
    boolean isActive();

    boolean isWritable();

    /**
     * Get remote address of the underlying connected channel
     */
    EndPoint getRemoteAddress();

    /**
     * Write the message without waiting for the response
     */
    void writeAsync(ServiceRequestMessageOut serviceRequest) throws IOException;
}
