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

package org.bithon.agent.core.dispatcher.config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frankchen
 */
public class DispatcherConfig {

    private Map<String, Boolean> messageDebug = new HashMap<>();
    private DispatcherQueue queue;

    private String servers;

    private DispatcherClient client;

    public DispatcherQueue getQueue() {
        return queue;
    }

    public void setQueue(DispatcherQueue queue) {
        this.queue = queue;
    }

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public DispatcherClient getClient() {
        return client;
    }

    public void setClient(DispatcherClient client) {
        this.client = client;
    }

    public Map<String, Boolean> getMessageDebug() {
        return messageDebug;
    }

    public void setMessageDebug(Map<String, Boolean> messageDebug) {
        this.messageDebug = messageDebug;
    }
}
