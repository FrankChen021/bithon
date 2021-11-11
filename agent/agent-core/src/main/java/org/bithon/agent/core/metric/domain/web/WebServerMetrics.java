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

package org.bithon.agent.core.metric.domain.web;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 10:51 下午
 */
public class WebServerMetrics {

    private final WebServerType serverType;
    private final long connectionCount;
    private final long maxConnections;
    private final long activeThreads;
    private final long maxThreads;

    public WebServerMetrics(WebServerType serverType,
                            long connectionCount,
                            long maxConnections,
                            long activeThreads,
                            long maxThreads) {
        this.serverType = serverType;
        this.connectionCount = connectionCount;
        this.maxConnections = maxConnections;
        this.activeThreads = activeThreads;
        this.maxThreads = maxThreads;
    }

    public WebServerType getServerType() {
        return serverType;
    }

    public long getConnectionCount() {
        return connectionCount;
    }

    public long getMaxConnections() {
        return maxConnections;
    }

    public long getActiveThreads() {
        return activeThreads;
    }

    public long getMaxThreads() {
        return maxThreads;
    }
}
