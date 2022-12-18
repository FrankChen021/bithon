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

import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/10 14:10
 */
public class ClientChannelBuilder {
    private IEndPointProvider endpointProvider;
    private int workerThreads = 1;

    private int maxRetry = 30;
    private Duration retryInterval = Duration.ofMillis(100);

    private String appName;

    public static ClientChannelBuilder builder() {
        return new ClientChannelBuilder();
    }

    public ClientChannelBuilder endpointProvider(String host, int port) {
        this.endpointProvider = new SingleEndPointProvider(host, port);
        return this;
    }

    public ClientChannelBuilder endpointProvider(IEndPointProvider endPointProvider) {
        this.endpointProvider = endPointProvider;
        return this;
    }

    public ClientChannelBuilder workerThreads(int nWorkerThreads) {
        this.workerThreads = nWorkerThreads;
        return this;
    }

    public ClientChannelBuilder maxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
        return this;
    }

    public ClientChannelBuilder retryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public ClientChannelBuilder applicationName(String appName) {
        this.appName = appName;
        return this;
    }

    public ClientChannel build() {
        return new ClientChannel(endpointProvider, workerThreads, maxRetry, retryInterval, appName);
    }
}
