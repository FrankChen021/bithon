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

import org.bithon.component.brpc.endpoint.IEndPointProvider;
import org.bithon.component.brpc.endpoint.SingleEndPointProvider;
import org.bithon.shaded.io.netty.channel.WriteBufferWaterMark;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/10 14:10
 */
public class BrpcClientBuilder {
    boolean keepAlive = true;
    int sendBufferSize = 8192;
    int lowMaterMark = WriteBufferWaterMark.DEFAULT.low();
    int highMaterMark = 1024 * 1024;
    IEndPointProvider server;
    int workerThreads = 1;

    int maxRetry = 30;
    Duration retryInterval = Duration.ofMillis(100);

    String appName = "brpc-client";

    Map<String, String> headers;

    /**
     * The name that is used to set to threads of this client.
     * Although the default name is the same as {@link #appName} above,
     * it differs from it because one application might have more than one brpc client to serve different needs,
     * to mark the difference of these clients, this client id helps.
     */
    String clientId = "brpc-client";

    /**
     * The default value is 200ms, which is originally used in previous versions.
     * We keep it as compatibility.
     */
    int connectionTimeout = 200;

    public static BrpcClientBuilder builder() {
        return new BrpcClientBuilder();
    }

    public BrpcClientBuilder server(String host, int port) {
        this.server = new SingleEndPointProvider(host, port);
        return this;
    }

    public BrpcClientBuilder server(IEndPointProvider server) {
        this.server = server;
        return this;
    }

    public BrpcClientBuilder workerThreads(int nWorkerThreads) {
        this.workerThreads = nWorkerThreads;
        return this;
    }

    public BrpcClientBuilder maxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
        return this;
    }

    public BrpcClientBuilder retryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public BrpcClientBuilder applicationName(String appName) {
        this.appName = appName;
        return this;
    }

    public BrpcClientBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public BrpcClientBuilder header(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, value);
        return this;
    }

    public BrpcClientBuilder connectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public BrpcClientBuilder keepAlive(boolean keepAlive) {
        return this;
    }

    public BrpcClientBuilder sendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public BrpcClientBuilder lowMaterMark(int low) {
        this.lowMaterMark = low;
        return this;
    }

    public BrpcClientBuilder highMaterMark(int high) {
        this.highMaterMark = high;
        return this;
    }

    public BrpcClient build() {
        return new BrpcClient(this);
    }
}
