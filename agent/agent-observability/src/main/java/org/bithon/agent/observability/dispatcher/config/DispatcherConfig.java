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

import org.bithon.agent.config.RpcClientConfig;

import java.util.Collections;
import java.util.Map;

/**
 * @author frankchen
 */
public class DispatcherConfig {

    private Map<String, Boolean> messageDebug = Collections.emptyMap();

    public enum QueueFullStrategy {
        DISCARD_NEWEST,
        DISCARD_OLDEST
    }

    /**
     * The upper limit of the message queue size.
     * Considering to set it to a proper size that matches the concurrency of your target application.
     */
    private int queueSize = 1024;

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    private QueueFullStrategy queueFullStrategy = QueueFullStrategy.DISCARD_OLDEST;

    private String servers;

    private RpcClientConfig client;

    /**
     * The size of a batch that messages are bundled to send.
     * Can be zero.
     */
    private int batchSize = 0;

    /**
     * The time interval in milliseconds to flush messages.
     * The dispatcher waits up to this configured time to prepare a batch to send,
     * When the messages are sent is determined by this time and {@link #batchSize}, whichever first arrives.
     */
    private int flushTime = 10;

    public String getServers() {
        return servers;
    }

    public void setServers(String servers) {
        this.servers = servers;
    }

    public RpcClientConfig getClient() {
        return client;
    }

    public void setClient(RpcClientConfig client) {
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

    public QueueFullStrategy getQueueFullStrategy() {
        return queueFullStrategy;
    }

    public void setQueueFullStrategy(QueueFullStrategy queueFullStrategy) {
        this.queueFullStrategy = queueFullStrategy;
    }
}
