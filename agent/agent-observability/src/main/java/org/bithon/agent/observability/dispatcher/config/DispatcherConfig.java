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

package org.bithon.agent.observability.dispatcher.config;

import java.util.Collections;
import java.util.Map;

/**
 * @author frankchen
 */
public class DispatcherConfig {

    private Map<String, Boolean> messageDebug = Collections.emptyMap();

    private int queueSize = 4096;

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    private String servers;

    private DispatcherClient client;

    private int batchSize = 0;

    /**
     * in milliseconds
     */
    private int flushTime = 10;

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

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getFlushTime() {
        return flushTime;
    }

    public void setFlushTime(int flushTime) {
        this.flushTime = flushTime;
    }
}
