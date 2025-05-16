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

import java.util.concurrent.Executor;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/30 22:18
 */
public class BrpcServerBuilder {
    String serverId;
    int idleSeconds = 180;
    int backlog = 1024;
    int ioThreads = Runtime.getRuntime().availableProcessors();
    int lowWaterMark = 0;
    int highWaterMark = 0;
    Executor executor = null;

    public static BrpcServerBuilder builder() {
        return new BrpcServerBuilder();
    }

    public BrpcServerBuilder serverId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public BrpcServerBuilder ioThreads(int ioThreads) {
        this.ioThreads = ioThreads;
        return this;
    }

    public BrpcServerBuilder idleSeconds(int idleSeconds) {
        this.idleSeconds = idleSeconds;
        return this;
    }

    public BrpcServerBuilder backlog(int backlog) {
        this.backlog = backlog;
        return this;
    }

    public BrpcServerBuilder lowWaterMark(int lowWaterMark) {
        this.lowWaterMark = lowWaterMark;
        return this;
    }

    public BrpcServerBuilder highWaterMark(int highWaterMark) {
        this.highWaterMark = highWaterMark;
        return this;
    }

    /**
     * The executor that executes the service call
     */
    public BrpcServerBuilder executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public BrpcServer build() {
        return new BrpcServer(this);
    }
}
