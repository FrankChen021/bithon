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

package org.bithon.server.discovery.client;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author frank.chen021@outlook.com
 * @date 18/5/24 8:28 pm
 */
@Data
public
class DiscoveredServiceInstance {
    private String host;
    private int port;

    @Nullable
    private String contextPath;

    public DiscoveredServiceInstance(String host, int port, @Nullable String contextPath) {
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
    }

    public String getURL() {
        return contextPath == null ? "http://" + host + ":" + port : "http://" + host + ":" + port + contextPath;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
