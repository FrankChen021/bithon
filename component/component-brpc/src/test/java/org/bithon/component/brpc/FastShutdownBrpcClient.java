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

package org.bithon.component.brpc;


import org.bithon.component.brpc.channel.BrpcClient;
import org.bithon.component.brpc.channel.BrpcClientBuilder;

/**
 * Helper wrapper for fast client shutdown in tests
 */
class FastShutdownBrpcClient implements AutoCloseable {
    private final BrpcClient client;

    public FastShutdownBrpcClient(String host, int port) {
        this.client = BrpcClientBuilder.builder().server(host, port).build();
    }

    public <T> T getRemoteService(Class<T> serviceType) {
        return client.getRemoteService(serviceType);
    }

    @Override
    public void close() {
        client.fastClose();
    }

    public boolean isActive() {
        return client.isActive();
    }
}
